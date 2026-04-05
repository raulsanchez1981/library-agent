"use server";

import { apiFetch } from "@/lib/api";
import type { AutorDetailDto, AutorDto } from "@/lib/types";

export async function fetchAutores(): Promise<AutorDto[]> {
  const res = await apiFetch("/api/v1/autores", { cache: "no-store" });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
  return res.json();
}

export async function fetchAutorDetail(id: string): Promise<AutorDetailDto> {
  const res = await apiFetch(`/api/v1/autores/${id}`, { cache: "no-store" });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
  return res.json();
}
