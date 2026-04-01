import NextAuth from "next-auth";
import Authentik from "next-auth/providers/authentik";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
  }
}

export const { handlers, signIn, signOut, auth } = NextAuth({
  providers: [Authentik],
  callbacks: {
    jwt({ token, account }) {
      // Preferimos id_token (siempre JWT) sobre access_token (puede ser opaco en Authentik)
      if (account?.id_token) {
        token.accessToken = account.id_token;
      } else if (account?.access_token) {
        token.accessToken = account.access_token;
      }
      return token;
    },
    session({ session, token }) {
      session.accessToken = token.accessToken as string | undefined;
      return session;
    },
  },
});
