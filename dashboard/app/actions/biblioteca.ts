"use server";

import { apiFetch } from "@/lib/api";
import type { VerifiedTitleDto } from "@/lib/types";

export async function fetchBiblioteca(): Promise<VerifiedTitleDto[]> {
  const res = await apiFetch("/api/v1/biblioteca", { cache: "no-store" });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
  return res.json();
}
