import { fetchBookDetail } from "@/app/actions/biblioteca";
import Link from "next/link";
import type { VerifiedTitleDetailDto } from "@/lib/types";
import CdlVerifyButton from "@/components/biblioteca/cdl-verify-button";
import BookCoverImage from "@/components/biblioteca/book-cover-image";

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
        <div className="flex-shrink-0 w-48">
          <div className="relative w-48 aspect-[2/3] rounded-xl overflow-hidden bg-zinc-100 shadow-md ring-1 ring-zinc-200">
            {book.coverUrl ? (
              <BookCoverImage src={book.coverUrl} alt={book.name} />
            ) : (
              <div className="flex h-full items-center justify-center p-4">
                <span className="text-center text-sm text-zinc-400 leading-snug">{book.name}</span>
              </div>
            )}
            <CdlDetailBadge book={book} />
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
          <CdlVerifyButton id={book.id} currentCdlUrl={book.casaDelLibroUrl} />
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
          {book.technicalSheet ? (
            <TechnicalSheet raw={book.technicalSheet} />
          ) : (book.publisher || book.publishedDate || book.pageCount || book.isbn) ? (
            <GoogleBooksSheet book={book} />
          ) : null}
        </div>
      </div>
    </div>
  );
}

function CdlDetailBadge({ book }: { book: VerifiedTitleDetailDto }) {
  const status = book.cdlAutoSearchStatus;

  if (status === "CONFIRMED") {
    return (
      <span title="Verificado" className="absolute bottom-2 right-2 flex h-6 w-6 items-center justify-center rounded-full bg-emerald-500 ring-2 ring-white shadow">
        <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3.5} d="M5 13l4 4L19 7" />
        </svg>
      </span>
    );
  }
  if (status === "AUTO") {
    return (
      <span title="Datos obtenidos automáticamente — pendiente de verificación" className="absolute bottom-2 right-2 flex h-6 w-6 items-center justify-center rounded-full bg-amber-400 ring-2 ring-white shadow">
        <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3.5} d="M5 13l4 4L19 7" />
        </svg>
      </span>
    );
  }
  if (status === "NOT_FOUND") {
    return (
      <span title="No se encontraron datos automáticamente" className="absolute bottom-2 right-2 flex h-6 w-6 items-center justify-center rounded-full bg-zinc-400 ring-2 ring-white shadow">
        <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3.5} d="M5 13l4 4L19 7" />
        </svg>
      </span>
    );
  }
  return null;
}

function GoogleBooksSheet({ book }: { book: VerifiedTitleDetailDto }) {
  const entries: [string, string][] = [];
  if (book.publisher) entries.push(["Editorial", book.publisher]);
  if (book.publishedDate) entries.push(["Fecha de publicación", book.publishedDate]);
  if (book.pageCount) entries.push(["Páginas", String(book.pageCount)]);
  if (book.isbn) entries.push(["ISBN", book.isbn]);

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
