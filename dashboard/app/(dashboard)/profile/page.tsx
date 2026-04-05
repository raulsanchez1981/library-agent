import { apiFetch } from "@/lib/api";
import ProfileForm from "@/components/profile/profile-form";
import type { ApiResponse, UserProfileDto } from "@/lib/types";

async function fetchProfile(): Promise<UserProfileDto> {
  const res = await apiFetch("/api/v1/profile", { cache: "no-store" });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
  const json: ApiResponse<UserProfileDto> = await res.json();
  return json.data;
}

export default async function ProfilePage() {
  const profile = await fetchProfile();

  return (
    <div className="px-8 py-8">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-zinc-900">Mi perfil lector</h1>
        <p className="mt-1 text-sm text-zinc-500">
          Tus preferencias de lectura — el motor de recomendaciones las usa para puntuarte libros
        </p>
      </div>

      <div className="max-w-lg rounded-xl border border-zinc-100 bg-white p-6">
        <p className="text-xs text-zinc-400 mb-6">{profile.email}</p>
        <ProfileForm profile={profile} />
      </div>
    </div>
  );
}
