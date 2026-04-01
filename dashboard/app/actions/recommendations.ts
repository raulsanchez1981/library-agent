"use server";

import { revalidatePath } from "next/cache";
import { apiFetch } from "@/lib/api";

export async function dismissRecommendation(id: string) {
  const res = await apiFetch(`/api/v1/recommendations/${id}/dismiss`, {
    method: "PATCH",
  });
  if (!res.ok) throw new Error("Error al descartar la recomendación");
  revalidatePath("/recommendations");
}

export async function triggerScoring(maxBatch: number = 20): Promise<{ processed: number }> {
  const res = await apiFetch(
    `/api/v1/recommendations/trigger?maxBatch=${maxBatch}`,
    { method: "POST" }
  );
  if (!res.ok) throw new Error("Error al lanzar el scoring");
  const data = await res.json();
  revalidatePath("/recommendations");
  return { processed: data.processed ?? 0 };
}
