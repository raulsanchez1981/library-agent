"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { updateAdminBook } from "@/app/actions/admin-books";
import type {
  Confidence,
  ExtractedBookAdminDto,
  SpringPage,
  UpdateExtractedBookRequest,
} from "@/lib/types";

interface Props {
  data: SpringPage<ExtractedBookAdminDto>;
  currentFilters: {
    search?: string;
    confidence?: string;
    enriched?: string;
  };
  currentSort: {
    sortBy: string;
    sortDir: string;
  };
  currentPage: number;
}

type SortField =
  | "title"
  | "titleEs"
  | "authorCorrected"
  | "confidence"
  | "enrichmentSource"
  | "enriched";

export default function BooksAdminClient({
  data,
  currentFilters,
  currentSort,
  currentPage,
}: Props) {
  const router = useRouter();
  const [editingBook, setEditingBook] = useState<ExtractedBookAdminDto | null>(null);

  function buildHref(overrides: Record<string, string | undefined>) {
    const params = new URLSearchParams();
    const merged: Record<string, string | undefined> = {
      ...currentFilters,
      sortBy: currentSort.sortBy,
      sortDir: currentSort.sortDir,
      page: String(currentPage),
      ...overrides,
    };
    Object.entries(merged).forEach(([k, v]) => {
      if (v !== undefined && v !== "") params.set(k, v);
    });
    return `/admin/books?${params}`;
  }

  function handleFilterChange(key: string, value: string) {
    router.push(buildHref({ [key]: value || undefined, page: "0" }));
  }

  function handleSearchSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const search = (
      e.currentTarget.elements.namedItem("search") as HTMLInputElement
    ).value;
    router.push(buildHref({ search: search || undefined, page: "0" }));
  }

  function handleSort(field: SortField) {
    const newDir =
      currentSort.sortBy === field && currentSort.sortDir === "asc" ? "desc" : "asc";
    router.push(buildHref({ sortBy: field, sortDir: newDir, page: "0" }));
  }

  return (
    <>
      {/* Filtros */}
      <div className="mb-4 flex flex-wrap gap-3">
        <form onSubmit={handleSearchSubmit} className="flex gap-2">
          <input
            name="search"
            defaultValue={currentFilters.search ?? ""}
            placeholder="Buscar por título o autor…"
            className="rounded-lg border border-zinc-200 px-3 py-2 text-sm w-64 focus:outline-none focus:ring-2 focus:ring-zinc-300"
          />
          <button
            type="submit"
            className="rounded-lg border border-zinc-200 px-3 py-2 text-sm hover:bg-zinc-50 transition-colors"
          >
            Buscar
          </button>
        </form>

        <select
          value={currentFilters.confidence ?? ""}
          onChange={(e) => handleFilterChange("confidence", e.target.value)}
          className="rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-300"
        >
          <option value="">Todas las confianzas</option>
          <option value="VERIFIED">VERIFIED</option>
          <option value="HIGH">HIGH</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="LOW">LOW</option>
        </select>

        <select
          value={currentFilters.enriched ?? ""}
          onChange={(e) => handleFilterChange("enriched", e.target.value)}
          className="rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-300"
        >
          <option value="">Todos</option>
          <option value="true">Enriquecidos</option>
          <option value="false">Sin enriquecer</option>
        </select>

        {(currentFilters.search || currentFilters.confidence || currentFilters.enriched) && (
          <a
            href="/admin/books"
            className="rounded-lg border border-zinc-200 px-3 py-2 text-sm text-zinc-500 hover:bg-zinc-50 transition-colors"
          >
            Limpiar filtros
          </a>
        )}
      </div>

      {/* Tabla */}
      <div className="overflow-x-auto rounded-xl border border-zinc-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-zinc-50 border-b border-zinc-200">
            <tr>
              <SortableHeader
                label="Título original"
                field="title"
                currentSort={currentSort}
                onSort={handleSort}
              />
              <SortableHeader
                label="Título ES"
                field="titleEs"
                currentSort={currentSort}
                onSort={handleSort}
              />
              <SortableHeader
                label="Autor"
                field="authorCorrected"
                currentSort={currentSort}
                onSort={handleSort}
              />
              <SortableHeader
                label="Confianza"
                field="confidence"
                currentSort={currentSort}
                onSort={handleSort}
              />
              <SortableHeader
                label="Fuente"
                field="enrichmentSource"
                currentSort={currentSort}
                onSort={handleSort}
              />
              <SortableHeader
                label="Estado"
                field="enriched"
                currentSort={currentSort}
                onSort={handleSort}
              />
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-zinc-100">
            {data.content.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-zinc-400">
                  No hay libros con los filtros seleccionados
                </td>
              </tr>
            ) : (
              data.content.map((book) => (
                <BookRow
                  key={book.id}
                  book={book}
                  onEdit={() => setEditingBook(book)}
                />
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Paginación */}
      {data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between text-sm text-zinc-500">
          <span>
            Página {currentPage + 1} de {data.totalPages}
          </span>
          <div className="flex gap-2">
            {!data.first && (
              <a
                href={buildHref({ page: String(currentPage - 1) })}
                className="rounded-lg border border-zinc-200 px-3 py-1.5 hover:bg-zinc-50 transition-colors"
              >
                Anterior
              </a>
            )}
            {!data.last && (
              <a
                href={buildHref({ page: String(currentPage + 1) })}
                className="rounded-lg border border-zinc-200 px-3 py-1.5 hover:bg-zinc-50 transition-colors"
              >
                Siguiente
              </a>
            )}
          </div>
        </div>
      )}

      {/* Modal de edición */}
      {editingBook && (
        <EditBookModal
          book={editingBook}
          onClose={() => setEditingBook(null)}
          onSaved={() => {
            setEditingBook(null);
            router.refresh();
          }}
        />
      )}
    </>
  );
}

function SortableHeader({
  label,
  field,
  currentSort,
  onSort,
}: {
  label: string;
  field: SortField;
  currentSort: { sortBy: string; sortDir: string };
  onSort: (field: SortField) => void;
}) {
  const isActive = currentSort.sortBy === field;
  const isAsc = isActive && currentSort.sortDir === "asc";

  return (
    <th className="px-4 py-3 text-left">
      <button
        onClick={() => onSort(field)}
        className="flex items-center gap-1 font-medium text-zinc-600 hover:text-zinc-900 transition-colors group"
      >
        {label}
        <span className="text-zinc-300 group-hover:text-zinc-500 transition-colors">
          {isActive ? (
            isAsc ? (
              <svg className="w-3.5 h-3.5 text-zinc-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 15l7-7 7 7" />
              </svg>
            ) : (
              <svg className="w-3.5 h-3.5 text-zinc-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M19 9l-7 7-7-7" />
              </svg>
            )
          ) : (
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 9l4-4 4 4M16 15l-4 4-4-4" />
            </svg>
          )}
        </span>
      </button>
    </th>
  );
}

function BookRow({
  book,
  onEdit,
}: {
  book: ExtractedBookAdminDto;
  onEdit: () => void;
}) {
  const linkedAuthors = book.authors.length > 0 ? book.authors.join(", ") : null;
  const displayAuthor = linkedAuthors ?? book.authorCorrected ?? book.author ?? "—";

  return (
    <tr className="hover:bg-zinc-50 transition-colors">
      <td className="px-4 py-3 text-zinc-800 max-w-xs">
        <p className="font-medium truncate" title={book.title}>
          {book.title}
        </p>
        {book.isSaga && <span className="text-xs text-zinc-400">saga</span>}
      </td>
      <td className="px-4 py-3 max-w-xs">
        {book.verifiedTitleName ? (
          <>
            <p className="truncate font-medium text-violet-700" title={book.verifiedTitleName}>
              {book.verifiedTitleName}
            </p>
            {book.titleEs && book.titleEs.toLowerCase() !== book.verifiedTitleName.toLowerCase() && (
              <p className="text-xs text-zinc-400 truncate" title={book.titleEs}>
                enriq.: {book.titleEs}
              </p>
            )}
          </>
        ) : book.titleEs ? (
          <>
            <p className="truncate text-zinc-600" title={book.titleEs}>
              {book.titleEs}
            </p>
            {book.titleEsOl && book.titleEsOl !== book.titleEs && (
              <p className="text-xs text-zinc-400 truncate" title={book.titleEsOl}>
                OL: {book.titleEsOl}
              </p>
            )}
          </>
        ) : (
          <span className="text-zinc-300">—</span>
        )}
      </td>
      <td className="px-4 py-3 max-w-xs">
        {linkedAuthors ? (
          <p className="truncate font-medium text-violet-700" title={linkedAuthors}>
            {linkedAuthors}
          </p>
        ) : (
          <>
            <p className="truncate text-zinc-600" title={displayAuthor}>
              {displayAuthor}
            </p>
            {book.authorCorrected && book.author && book.authorCorrected !== book.author && (
              <p className="text-xs text-zinc-400 truncate" title={book.author}>
                orig: {book.author}
              </p>
            )}
          </>
        )}
      </td>
      <td className="px-4 py-3">
        <ConfidenceBadge confidence={book.confidence} />
      </td>
      <td className="px-4 py-3 text-zinc-500 text-xs">{book.enrichmentSource ?? "—"}</td>
      <td className="px-4 py-3">
        {book.enriched ? (
          <span className="inline-flex items-center gap-1 text-xs text-emerald-600">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
            Enriquecido
          </span>
        ) : (
          <span className="inline-flex items-center gap-1 text-xs text-zinc-400">
            <span className="h-1.5 w-1.5 rounded-full bg-zinc-300" />
            Pendiente
          </span>
        )}
      </td>
      <td className="px-4 py-3 text-right">
        <button
          onClick={onEdit}
          className="text-xs text-zinc-500 hover:text-zinc-900 underline-offset-2 hover:underline transition-colors"
        >
          Editar
        </button>
      </td>
    </tr>
  );
}

function ConfidenceBadge({ confidence }: { confidence: Confidence | null }) {
  if (!confidence) return <span className="text-zinc-300 text-xs">—</span>;

  const styles: Record<Confidence, string> = {
    VERIFIED: "bg-violet-50 text-violet-700 border-violet-200",
    HIGH: "bg-emerald-50 text-emerald-700 border-emerald-200",
    MEDIUM: "bg-amber-50 text-amber-700 border-amber-200",
    LOW: "bg-red-50 text-red-700 border-red-200",
  };

  return (
    <span
      className={`inline-block rounded border px-1.5 py-0.5 text-xs font-medium ${styles[confidence]}`}
    >
      {confidence}
    </span>
  );
}

function EditBookModal({
  book,
  onClose,
  onSaved,
}: {
  book: ExtractedBookAdminDto;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;

    const titleEs = (form.elements.namedItem("titleEs") as HTMLInputElement).value.trim();
    const authorCorrected = (
      form.elements.namedItem("authorCorrected") as HTMLInputElement
    ).value.trim();
    const availableInSpanish = (
      form.elements.namedItem("availableInSpanish") as HTMLInputElement
    ).checked;
    const isSaga = (form.elements.namedItem("isSaga") as HTMLInputElement).checked;

    const payload: UpdateExtractedBookRequest = {
      titleEs: titleEs || null,
      authorCorrected: authorCorrected || null,
      availableInSpanish,
      isSaga,
    };

    setError(null);
    startTransition(async () => {
      try {
        await updateAdminBook(book.id, payload);
        onSaved();
      } catch (err) {
        setError(err instanceof Error ? err.message : "Error desconocido");
      }
    });
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="w-full max-w-lg rounded-xl bg-white shadow-xl mx-4">
        {/* Cabecera */}
        <div className="flex items-start justify-between border-b border-zinc-200 px-6 py-4">
          <div>
            <h2 className="font-semibold text-zinc-900">Editar libro</h2>
            <p className="text-xs text-zinc-400 mt-0.5 max-w-sm truncate" title={book.title}>
              {book.title}
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-zinc-400 hover:text-zinc-600 transition-colors ml-4"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        {/* Datos de referencia (solo lectura) */}
        <div className="px-6 pt-4 pb-3 grid grid-cols-2 gap-3 text-xs bg-zinc-50 border-b border-zinc-100">
          <div>
            <span className="font-medium text-zinc-400 uppercase tracking-wide">Autor original</span>
            <p className="mt-0.5 text-zinc-700">{book.author ?? "—"}</p>
          </div>
          <div>
            <span className="font-medium text-zinc-400 uppercase tracking-wide">Título OL</span>
            <p className="mt-0.5 text-zinc-700">{book.titleEsOl ?? "—"}</p>
          </div>
          <div>
            <span className="font-medium text-zinc-400 uppercase tracking-wide">Confianza actual</span>
            <p className="mt-0.5">
              <ConfidenceBadge confidence={book.confidence} />
            </p>
          </div>
          <div>
            <span className="font-medium text-zinc-400 uppercase tracking-wide">Fuente actual</span>
            <p className="mt-0.5 text-zinc-700">{book.enrichmentSource ?? "—"}</p>
          </div>
          {book.verifiedTitleName && (
            <div className="col-span-2">
              <span className="font-medium text-zinc-400 uppercase tracking-wide">Título verificado</span>
              <p className="mt-0.5 font-medium text-violet-700">{book.verifiedTitleName}</p>
            </div>
          )}
          {book.authors.length > 0 && (
            <div className="col-span-2">
              <span className="font-medium text-zinc-400 uppercase tracking-wide">Autores vinculados</span>
              <div className="mt-1 flex flex-wrap gap-1">
                {book.authors.map((a) => (
                  <span
                    key={a}
                    className="inline-block rounded border border-violet-200 bg-violet-50 px-1.5 py-0.5 text-violet-700"
                  >
                    {a}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Formulario */}
        <form onSubmit={handleSubmit} className="px-6 py-4 space-y-4">
          <div>
            <label className="block text-sm font-medium text-zinc-700 mb-1">
              Título en español
            </label>
            <input
              name="titleEs"
              defaultValue={book.titleEs ?? ""}
              className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-300"
              placeholder="Título traducido al español"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-zinc-700 mb-1">
              Autor corregido
            </label>
            <input
              name="authorCorrected"
              defaultValue={book.authorCorrected ?? book.author ?? ""}
              className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-300"
              placeholder="Nombre correcto del autor"
            />
          </div>

          <div className="flex gap-6">
            <label className="flex items-center gap-2 text-sm text-zinc-700 cursor-pointer">
              <input
                name="availableInSpanish"
                type="checkbox"
                defaultChecked={book.availableInSpanish}
                className="rounded border-zinc-300"
              />
              Disponible en español
            </label>
            <label className="flex items-center gap-2 text-sm text-zinc-700 cursor-pointer">
              <input
                name="isSaga"
                type="checkbox"
                defaultChecked={book.isSaga}
                className="rounded border-zinc-300"
              />
              Es una saga
            </label>
          </div>

          {/* Aviso de sellado automático */}
          <p className="text-xs text-violet-600 bg-violet-50 rounded-lg px-3 py-2">
            Al guardar, el libro quedará marcado como <strong>VERIFIED</strong> con fuente <strong>ADMIN</strong>.
          </p>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <div className="flex justify-end gap-3 pt-1">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-zinc-200 px-4 py-2 text-sm text-zinc-600 hover:bg-zinc-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isPending}
              className="rounded-lg bg-zinc-900 px-4 py-2 text-sm text-white hover:bg-zinc-700 disabled:opacity-50 transition-colors"
            >
              {isPending ? "Guardando…" : "Guardar y verificar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
