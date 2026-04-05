"use client";

import { useTransition, useState, useEffect, useRef } from "react";
import { addHistoryEntry } from "@/app/actions/history";
import { searchBooks } from "@/app/actions/books";
import type { BookSearchResultDto, ReadingStatus } from "@/lib/types";

const STATUS_LABELS: Record<ReadingStatus, string> = {
  PENDING: "Pendiente",
  IN_PROGRESS: "En curso",
  READ: "Leído",
  ABANDONED: "Abandonado",
};

function BookAutocomplete({
  value,
  onChange,
  onSelect,
}: {
  value: string;
  onChange: (v: string) => void;
  onSelect: (book: BookSearchResultDto) => void;
}) {
  const [suggestions, setSuggestions] = useState<BookSearchResultDto[]>([]);
  const [searching, setSearching] = useState(false);
  const [open, setOpen] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);

    if (value.trim().length < 2) {
      setSuggestions([]);
      setOpen(false);
      setSearching(false);
      return;
    }

    setSearching(true);
    debounceRef.current = setTimeout(async () => {
      try {
        const results = await searchBooks(value);
        setSuggestions(results);
        setOpen(true); // abre siempre para mostrar "sin resultados" si procede
      } catch {
        setSuggestions([]);
        setOpen(false);
      } finally {
        setSearching(false);
      }
    }, 300);

    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [value]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const showDropdown = open && value.trim().length >= 2 && !searching;

  return (
    <div ref={containerRef} className="relative flex-1">
      <div className="relative">
        <input
          type="text"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onFocus={() => suggestions.length > 0 && setOpen(true)}
          placeholder="Título del libro *"
          required
          className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-900"
        />
        {searching && (
          <span className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-zinc-400">
            …
          </span>
        )}
      </div>

      {showDropdown && (
        <ul className="absolute z-20 mt-1 w-full rounded-lg border border-zinc-200 bg-white shadow-lg max-h-56 overflow-y-auto">
          {suggestions.length === 0 ? (
            <li className="px-3 py-2 text-xs text-zinc-400">
              Sin resultados — se guardará tal como lo escribas
            </li>
          ) : (
            suggestions.map((book) => (
              <li key={book.id}>
                <button
                  type="button"
                  onMouseDown={(e) => {
                    e.preventDefault();
                    onSelect(book);
                    setOpen(false);
                    setSuggestions([]);
                  }}
                  className="w-full px-3 py-2 text-left hover:bg-zinc-50 transition-colors"
                >
                  <p className="text-sm font-medium text-zinc-900 truncate">
                    {book.titleEs ?? book.title}
                  </p>
                  {book.author && (
                    <p className="text-xs text-zinc-500 truncate">{book.author}</p>
                  )}
                  {book.titleEs && book.titleEs !== book.title && (
                    <p className="text-xs text-zinc-400 truncate">{book.title}</p>
                  )}
                </button>
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  );
}

export default function AddHistoryForm() {
  const [isPending, startTransition] = useTransition();
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [title, setTitle] = useState("");
  const [author, setAuthor] = useState("");
  const [status, setStatus] = useState<ReadingStatus>("PENDING");

  function handleSelectBook(book: BookSearchResultDto) {
    setTitle(book.titleEs ?? book.title);
    setAuthor(book.author ?? "");
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setError(null);

    startTransition(async () => {
      try {
        await addHistoryEntry({
          bookTitle: title.trim(),
          bookAuthor: author.trim() || null,
          status,
        });
        setTitle("");
        setAuthor("");
        setStatus("PENDING");
        setOpen(false);
      } catch {
        setError("No se pudo añadir el libro. Inténtalo de nuevo.");
      }
    });
  }

  if (!open) {
    return (
      <button
        onClick={() => setOpen(true)}
        className="flex items-center gap-2 rounded-lg border border-dashed border-zinc-300 px-4 py-2 text-sm text-zinc-500 hover:border-zinc-400 hover:text-zinc-700 transition-colors"
      >
        <span className="text-base leading-none">+</span> Añadir libro
      </button>
    );
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-xl border border-zinc-200 bg-white p-4 space-y-3"
    >
      <div className="flex gap-3">
        <BookAutocomplete
          value={title}
          onChange={setTitle}
          onSelect={handleSelectBook}
        />
        <input
          type="text"
          value={author}
          onChange={(e) => setAuthor(e.target.value)}
          placeholder="Autor (opcional)"
          className="flex-1 rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-900"
        />
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as ReadingStatus)}
          className="rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-900"
        >
          {(Object.keys(STATUS_LABELS) as ReadingStatus[]).map((s) => (
            <option key={s} value={s}>
              {STATUS_LABELS[s]}
            </option>
          ))}
        </select>
      </div>

      {error && <p className="text-xs text-red-600">{error}</p>}

      <div className="flex gap-2">
        <button
          type="submit"
          disabled={isPending || !title.trim()}
          className="rounded-lg bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-800 disabled:opacity-50 transition-colors"
        >
          {isPending ? "Añadiendo…" : "Añadir"}
        </button>
        <button
          type="button"
          onClick={() => {
            setOpen(false);
            setError(null);
          }}
          className="rounded-lg border border-zinc-200 px-4 py-2 text-sm text-zinc-600 hover:bg-zinc-50 transition-colors"
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}
