"use server";

import { apiFetch } from "@/lib/api";
import { revalidatePath } from "next/cache";
import type {
  ExtractedBookAdminDto,
  SpringPage,
  UpdateExtractedBookRequest,
} from "@/lib/types";

export async function fetchAdminBooks(params: {
  search?: string;
  confidence?: string;
  enriched?: string;
  sortBy?: string;
  sortDir?: string;
  page?: number;
  size?: number;
}): Promise<SpringPage<ExtractedBookAdminDto>> {
  const query = new URLSearchParams();
  if (params.search) query.set("search", params.search);
  if (params.confidence) query.set("confidence", params.confidence);
  if (params.enriched !== undefined && params.enriched !== "")
    query.set("enriched", params.enriched);
  if (params.sortBy) query.set("sortBy", params.sortBy);
  if (params.sortDir) query.set("sortDir", params.sortDir);
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 20));

  const res = await apiFetch(`/api/v1/admin/books?${query}`, {
    cache: "no-store",
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
  return res.json();
}

export async function updateAdminBook(
  id: string,
  data: UpdateExtractedBookRequest
): Promise<ExtractedBookAdminDto> {
  const res = await apiFetch(`/api/v1/admin/books/${id}`, {
    method: "PATCH",
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Error al actualizar el libro");
  revalidatePath("/admin/books");
  return res.json();
}
