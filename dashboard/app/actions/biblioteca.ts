"use server";

import { apiFetch } from "@/lib/api";
import { revalidatePath } from "next/cache";
import type { VerifiedTitleDetailDto, VerifiedTitleDto } from "@/lib/types";

export async function fetchBiblioteca(): Promise<VerifiedTitleDto[]> {
  const res = await apiFetch("/api/v1/biblioteca", { cache: "no-store" });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
  return res.json();
}

export async function fetchBookDetail(id: string): Promise<VerifiedTitleDetailDto> {
  const res = await apiFetch(`/api/v1/biblioteca/${id}`, { cache: "no-store" });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
  return res.json();
}

export async function cdlAutoSearch(verifiedTitleIds: string[]): Promise<void> {
  const res = await apiFetch("/api/v1/admin/verified-titles/cdl-auto-search", {
    method: "POST",
    body: JSON.stringify({ verifiedTitleIds }),
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
}

export async function cdlAutoSearchAll(): Promise<void> {
  const res = await apiFetch("/api/v1/admin/verified-titles/cdl-auto-search-all", {
    method: "POST",
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
}

export async function reEnrichGoogleBooks(): Promise<void> {
  const res = await apiFetch("/api/v1/admin/verified-titles/re-enrich-google-books", {
    method: "POST",
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
}

export async function confirmBook(verifiedTitleId: string): Promise<VerifiedTitleDetailDto> {
  const res = await apiFetch(
    `/api/v1/admin/verified-titles/${verifiedTitleId}/confirm`,
    { method: "POST" }
  );
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
  revalidatePath("/biblioteca");
  revalidatePath(`/biblioteca/${verifiedTitleId}`);
  return res.json();
}

export async function enrichFromCdl(
  verifiedTitleId: string,
  casaDelLibroUrl: string
): Promise<VerifiedTitleDetailDto> {
  const res = await apiFetch(
    `/api/v1/admin/verified-titles/${verifiedTitleId}/enrich-cdl`,
    {
      method: "POST",
      body: JSON.stringify({ casaDelLibroUrl }),
    }
  );
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
  revalidatePath("/biblioteca");
  revalidatePath(`/biblioteca/${verifiedTitleId}`);
  return res.json();
}
