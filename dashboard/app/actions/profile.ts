"use server";

import { apiFetch } from "@/lib/api";
import { revalidatePath } from "next/cache";

export async function updateProfile(data: {
  preferredLanguage: string;
  minScoreThreshold: number;
  favoriteGenres: string[];
  favoriteAuthors: string[];
}) {
  const res = await apiFetch("/api/v1/profile", {
    method: "PUT",
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Error al actualizar el perfil");
  revalidatePath("/profile");
}
