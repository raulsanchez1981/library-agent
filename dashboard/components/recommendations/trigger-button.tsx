"use client";

import { useTransition, useState } from "react";
import { triggerScoring } from "@/app/actions/recommendations";

export default function TriggerButton() {
  const [isPending, startTransition] = useTransition();
  const [result, setResult] = useState<string | null>(null);

  function handleClick() {
    setResult(null);
    startTransition(async () => {
      try {
        const { processed } = await triggerScoring(20);
        setResult(
          processed === 0
            ? "No hay libros nuevos para puntuar"
            : `${processed} libro${processed !== 1 ? "s" : ""} procesado${processed !== 1 ? "s" : ""}`
        );
      } catch {
        setResult("Error al lanzar el scoring");
      }
    });
  }

  return (
    <div className="flex items-center gap-3">
      <button
        onClick={handleClick}
        disabled={isPending}
        className="inline-flex items-center gap-2 rounded-lg bg-zinc-900 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-zinc-700 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isPending ? (
          <>
            <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
            </svg>
            Procesando…
          </>
        ) : (
          <>
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            Lanzar scoring
          </>
        )}
      </button>
      {result && (
        <span className="text-sm text-zinc-500">{result}</span>
      )}
    </div>
  );
}
