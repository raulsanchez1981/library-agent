"use server";

import { apiFetch } from "@/lib/api";
import { revalidatePath } from "next/cache";
import type { ReadingStatus } from "@/lib/types";

export async function addHistoryEntry(data: {
  bookTitle: string;
  bookAuthor: string | null;
  status: ReadingStatus;
}) {
  const res = await apiFetch("/api/v1/reading-history", {
    method: "POST",
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Error al añadir el libro al historial");
  revalidatePath("/history");
}

export async function updateHistoryEntry(
  id: string,
  data: {
    bookTitle?: string;
    bookAuthor?: string | null;
    status?: ReadingStatus;
  }
) {
  const res = await apiFetch(`/api/v1/reading-history/${id}`, {
    method: "PATCH",
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Error al actualizar la entrada");
  revalidatePath("/history");
}

export async function deleteHistoryEntry(id: string) {
  const res = await apiFetch(`/api/v1/reading-history/${id}`, {
    method: "DELETE",
  });
  if (!res.ok) throw new Error("Error al eliminar la entrada");
  revalidatePath("/history");
}
