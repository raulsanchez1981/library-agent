"use client";

import { useOptimistic, useTransition } from "react";
import { dismissRecommendation } from "@/app/actions/recommendations";
import ScoreBadge from "./score-badge";
import type { RecommendationDto } from "@/lib/types";

export default function RecommendationList({
  recommendations,
}: {
  recommendations: RecommendationDto[];
}) {
  const [optimistic, removeOptimistic] = useOptimistic(
    recommendations,
    (state, dismissedId: string) => state.filter((r) => r.id !== dismissedId)
  );
  const [, startTransition] = useTransition();

  function handleDismiss(id: string) {
    startTransition(async () => {
      removeOptimistic(id);
      await dismissRecommendation(id);
    });
  }

  if (optimistic.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-zinc-200 bg-white p-12 text-center">
        <span className="text-4xl">✨</span>
        <p className="mt-4 text-sm font-medium text-zinc-500">
          No hay recomendaciones pendientes
        </p>
        <p className="mt-1 text-xs text-zinc-400">
          Lanza el scoring para generar nuevas recomendaciones
        </p>
      </div>
    );
  }

  return (
    <ul className="space-y-3">
      {optimistic.map((rec) => (
        <li
          key={rec.id}
          className="flex items-start gap-4 rounded-xl border border-zinc-100 bg-white px-5 py-4 shadow-sm"
        >
          {/* Score */}
          <div className="mt-0.5 shrink-0">
            <ScoreBadge score={rec.score} />
          </div>

          {/* Contenido */}
          <div className="min-w-0 flex-1">
            <p className="font-medium text-zinc-900 truncate">
              {rec.bookTitle}
            </p>
            {rec.bookAuthor && (
              <p className="text-sm text-zinc-500">{rec.bookAuthor}</p>
            )}
            <p className="mt-2 text-sm text-zinc-600 leading-relaxed">
              {rec.reasoning}
            </p>
          </div>

          {/* Botón descartar */}
          <button
            onClick={() => handleDismiss(rec.id)}
            title="Descartar"
            className="mt-0.5 shrink-0 rounded-lg p-1.5 text-zinc-400 transition-colors hover:bg-zinc-100 hover:text-zinc-600"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </li>
      ))}
    </ul>
  );
}
