import NextAuth from "next-auth";
import Authentik from "next-auth/providers/authentik";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    isAdmin?: boolean;
    error?: string;
  }
}

let cachedTokenEndpoint: string | null = null;

async function getTokenEndpoint(): Promise<string> {
  if (cachedTokenEndpoint) return cachedTokenEndpoint;
  const issuer = process.env.AUTH_AUTHENTIK_ISSUER!.replace(/\/$/, "");
  const res = await fetch(`${issuer}/.well-known/openid-configuration`);
  const config = await res.json();
  cachedTokenEndpoint = config.token_endpoint as string;
  return cachedTokenEndpoint;
}

async function refreshAccessToken(refreshToken: string) {
  const tokenEndpoint = await getTokenEndpoint();
  const response = await fetch(tokenEndpoint, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: process.env.AUTH_AUTHENTIK_ID!,
      client_secret: process.env.AUTH_AUTHENTIK_SECRET!,
      grant_type: "refresh_token",
      refresh_token: refreshToken,
      scope: "openid",
    }),
  });

  if (!response.ok) throw new Error(`Token refresh failed: ${response.status}`);
  return response.json();
}

export const { handlers, signIn, signOut, auth } = NextAuth({
  providers: [Authentik],
  callbacks: {
    async jwt({ token, account, profile }) {
      // Primer login: guardar todos los tokens con su expiración
      if (account) {
        token.accessToken = account.id_token ?? account.access_token;
        token.refreshToken = account.refresh_token;
        token.expiresAt = account.expires_at ?? Math.floor(Date.now() / 1000) + 3600;
        if (profile) {
          const groups = (profile as { groups?: string[] }).groups ?? [];
          token.isAdmin = groups.includes("library-admin");
        }
        return token;
      }

      // Token aún válido (margen de 60 s para evitar race conditions)
      if (Date.now() < (token.expiresAt as number) * 1000 - 60_000) {
        return token;
      }

      // Sin refresh token no podemos renovar
      if (!token.refreshToken) {
        return { ...token, error: "RefreshAccessTokenError" };
      }

      // Token expirado: renovar contra Authentik
      try {
        const refreshed = await refreshAccessToken(token.refreshToken as string);
        return {
          ...token,
          // id_token preferido (siempre JWT); access_token puede ser opaco en Authentik
          accessToken: refreshed.id_token ?? refreshed.access_token,
          refreshToken: refreshed.refresh_token ?? token.refreshToken,
          expiresAt: Math.floor(Date.now() / 1000) + (refreshed.expires_in as number),
          error: undefined,
        };
      } catch {
        return { ...token, error: "RefreshAccessTokenError" };
      }
    },
    session({ session, token }) {
      session.accessToken = token.accessToken as string | undefined;
      session.isAdmin = token.isAdmin as boolean | undefined;
      if (token.error) {
        session.error = token.error as string;
      }
      return session;
    },
  },
});
