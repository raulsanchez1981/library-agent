"use client";

import { useTransition, useState } from "react";
import { enrichAllGenres } from "@/app/actions/biblioteca";

export default function EnrichGenresButton() {
  const [isPending, startTransition] = useTransition();
  const [status, setStatus] = useState<"idle" | "processing" | "done">("idle");

  function handleClick() {
    setStatus("processing");
    startTransition(async () => {
      try {
        await enrichAllGenres();
        setStatus("done");
        setTimeout(() => setStatus("idle"), 4000);
      } catch {
        setStatus("idle");
      }
    });
  }

  return (
    <button
      onClick={handleClick}
      disabled={isPending || status === "processing"}
      className="inline-flex items-center gap-2 rounded-lg border border-zinc-200 bg-white px-3 py-1.5 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-50 disabled:opacity-50 disabled:cursor-not-allowed"
    >
      {status === "processing" ? (
        <>
          <svg className="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
          </svg>
          en proceso…
        </>
      ) : status === "done" ? (
        <>
          <svg className="w-3.5 h-3.5 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
          Lanzado
        </>
      ) : (
        <>
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
              d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-5 5a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 10V5a2 2 0 012-2z" />
          </svg>
          Enriquecer géneros
        </>
      )}
    </button>
  );
}
