import { apiFetch } from "@/lib/api";
import { Suspense } from "react";
import TriggerButton from "@/components/recommendations/trigger-button";
import RecommendationList from "@/components/recommendations/recommendation-list";
import type { RecommendationDto, SpringPage } from "@/lib/types";

async function fetchRecommendations(page: number): Promise<SpringPage<RecommendationDto>> {
  const res = await apiFetch(
    `/api/v1/recommendations?page=${page}&size=20&sort=score,desc`,
    { cache: "no-store" }
  );
  if (!res.ok) throw new Error("Error al cargar las recomendaciones");
  return res.json();
}

export default async function RecommendationsPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string }>;
}) {
  const { page: pageParam } = await searchParams;
  const page = Math.max(0, Number(pageParam ?? 0));

  const data = await fetchRecommendations(page);

  return (
    <div className="px-8 py-8">
      {/* Cabecera */}
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Recomendaciones</h1>
          <p className="mt-1 text-sm text-zinc-500">
            {data.totalElements > 0
              ? `${data.totalElements} libro${data.totalElements !== 1 ? "s" : ""} recomendado${data.totalElements !== 1 ? "s" : ""}`
              : "Libros seleccionados para ti por Claude según tu perfil lector"}
          </p>
        </div>
        <TriggerButton />
      </div>

      {/* Lista con UI optimista */}
      <Suspense fallback={<RecommendationsSkeleton />}>
        <RecommendationList recommendations={data.content} />
      </Suspense>

      {/* Paginación */}
      {data.totalPages > 1 && (
        <div className="mt-6 flex items-center justify-between text-sm text-zinc-500">
          <span>
            Página {page + 1} de {data.totalPages}
          </span>
          <div className="flex gap-2">
            {!data.first && (
              <a
                href={`/recommendations?page=${page - 1}`}
                className="rounded-lg border border-zinc-200 px-3 py-1.5 hover:bg-zinc-50 transition-colors"
              >
                Anterior
              </a>
            )}
            {!data.last && (
              <a
                href={`/recommendations?page=${page + 1}`}
                className="rounded-lg border border-zinc-200 px-3 py-1.5 hover:bg-zinc-50 transition-colors"
              >
                Siguiente
              </a>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function RecommendationsSkeleton() {
  return (
    <ul className="space-y-3">
      {Array.from({ length: 5 }).map((_, i) => (
        <li key={i} className="h-24 rounded-xl border border-zinc-100 bg-white animate-pulse" />
      ))}
    </ul>
  );
}
