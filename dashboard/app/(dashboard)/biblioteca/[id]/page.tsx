import { fetchBookDetail } from "@/app/actions/biblioteca";
import Image from "next/image";
import Link from "next/link";
import type { VerifiedTitleDetailDto } from "@/lib/types";

export default async function BookDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const book = await fetchBookDetail(id);

  return (
    <div className="px-8 py-8 max-w-4xl">
      <Link
        href="/biblioteca"
        className="inline-flex items-center gap-1.5 text-sm text-zinc-500 hover:text-zinc-900 transition-colors mb-6"
      >
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
        </svg>
        Biblioteca
      </Link>

      <div className="flex gap-8">
        {/* Portada */}
        <div className="flex-shrink-0">
          <div className="relative w-48 aspect-[2/3] rounded-xl overflow-hidden bg-zinc-100 shadow-md ring-1 ring-zinc-200">
            {book.coverUrl ? (
              <Image
                src={book.coverUrl}
                alt={book.name}
                fill
                className="object-cover"
                sizes="192px"
              />
            ) : (
              <div className="flex h-full items-center justify-center p-4">
                <span className="text-center text-sm text-zinc-400 leading-snug">{book.name}</span>
              </div>
            )}
          </div>
          {book.casaDelLibroUrl && (
            <a
              href={book.casaDelLibroUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-3 flex items-center justify-center gap-1.5 text-xs text-zinc-500 hover:text-zinc-900 transition-colors"
            >
              Ver en Casa del Libro
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          )}
        </div>

        {/* Contenido */}
        <div className="flex-1 min-w-0">
          <h1 className="text-2xl font-semibold text-zinc-900 leading-snug">{book.name}</h1>

          {/* Géneros */}
          {book.genres.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-1.5">
              {book.genres.map((g) => (
                <span
                  key={g.id}
                  className="inline-block rounded-full border border-violet-200 bg-violet-50 px-2.5 py-0.5 text-xs font-medium text-violet-700"
                >
                  {g.name}
                </span>
              ))}
            </div>
          )}

          {/* Sinopsis */}
          {book.synopsis && (
            <div className="mt-5">
              <h2 className="text-xs font-semibold uppercase tracking-wide text-zinc-400 mb-2">Sinopsis</h2>
              <p className="text-sm text-zinc-700 leading-relaxed whitespace-pre-line">{book.synopsis}</p>
            </div>
          )}

          {/* Ficha técnica */}
          {book.technicalSheet && (
            <TechnicalSheet raw={book.technicalSheet} />
          )}
        </div>
      </div>
    </div>
  );
}

function TechnicalSheet({ raw }: { raw: string }) {
  let entries: [string, string][] = [];
  try {
    const parsed = JSON.parse(raw) as Record<string, string>;
    entries = Object.entries(parsed);
  } catch {
    return null;
  }

  if (entries.length === 0) return null;

  return (
    <div className="mt-6">
      <h2 className="text-xs font-semibold uppercase tracking-wide text-zinc-400 mb-2">Ficha técnica</h2>
      <dl className="divide-y divide-zinc-100 rounded-xl border border-zinc-200 bg-white overflow-hidden">
        {entries.map(([key, value]) => (
          <div key={key} className="flex px-4 py-2.5 text-sm">
            <dt className="w-36 flex-shrink-0 font-medium text-zinc-500">{key}</dt>
            <dd className="text-zinc-800">{value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
}
