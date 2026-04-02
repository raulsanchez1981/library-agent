"use server";

import { apiFetch } from "@/lib/api";
import type { BookSearchResultDto } from "@/lib/types";

export async function searchBooks(q: string): Promise<BookSearchResultDto[]> {
  if (q.trim().length < 2) return [];

  const res = await apiFetch(
    `/api/v1/books/search?q=${encodeURIComponent(q.trim())}&limit=10`
  );
  if (!res.ok) return [];

  return res.json();
}
