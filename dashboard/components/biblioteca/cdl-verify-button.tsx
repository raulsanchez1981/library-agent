"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { confirmBook, enrichFromCdl } from "@/app/actions/biblioteca";

interface Props {
  id: string;
  currentCdlUrl: string | null;
}

export default function CdlVerifyButton({ id, currentCdlUrl }: Props) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [url, setUrl] = useState(currentCdlUrl ?? "");
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  function handleOpen() {
    setUrl(currentCdlUrl ?? "");
    setError(null);
    setOpen(true);
  }

  function handleConfirmWithCdl(e: React.FormEvent) {
    e.preventDefault();
    if (!url.trim()) return;
    setError(null);
    startTransition(async () => {
      try {
        await enrichFromCdl(id, url.trim());
        setOpen(false);
        router.refresh();
      } catch (err) {
        setError(err instanceof Error ? err.message : "Error al enriquecer con CDL");
      }
    });
  }

  function handleConfirmWithoutCdl() {
    setError(null);
    startTransition(async () => {
      try {
        await confirmBook(id);
        setOpen(false);
        router.refresh();
      } catch (err) {
        setError(err instanceof Error ? err.message : "Error al confirmar");
      }
    });
  }

  return (
    <>
      <button
        onClick={handleOpen}
        className="mt-3 flex items-center justify-center gap-1.5 w-full rounded-lg border border-zinc-300 bg-zinc-50 px-3 py-2 text-xs font-medium text-zinc-700 hover:bg-zinc-100 transition-colors"
      >
        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
        </svg>
        Verificar libro
      </button>

      {open && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
          onClick={(e) => e.target === e.currentTarget && setOpen(false)}
        >
          <div className="w-full max-w-md rounded-xl bg-white shadow-xl mx-4">
            <div className="flex items-center justify-between border-b border-zinc-200 px-6 py-4">
              <h2 className="font-semibold text-zinc-900">Verificar libro</h2>
              <button
                onClick={() => setOpen(false)}
                className="text-zinc-400 hover:text-zinc-600 transition-colors"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div className="px-6 py-5 space-y-5">
              {/* Sección URL CDL (opcional) */}
              <form onSubmit={handleConfirmWithCdl} className="space-y-3">
                <div>
                  <label className="block text-sm font-medium text-zinc-700 mb-1">
                    URL de Casa del Libro <span className="text-zinc-400 font-normal">(opcional)</span>
                  </label>
                  <input
                    type="url"
                    value={url}
                    onChange={(e) => setUrl(e.target.value)}
                    placeholder="https://www.casadellibro.com/libro-..."
                    className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-300"
                    autoFocus
                  />
                </div>
                <p className="text-xs text-zinc-400">
                  Si proporcionas una URL, se obtendrán portada, sinopsis y ficha técnica desde Casa del Libro.
                </p>
                <button
                  type="submit"
                  disabled={isPending || !url.trim()}
                  className="w-full rounded-lg bg-zinc-900 px-4 py-2 text-sm text-white hover:bg-zinc-700 disabled:opacity-50 transition-colors"
                >
                  {isPending ? "Obteniendo datos…" : "Confirmar con CDL"}
                </button>
              </form>

              {/* Separador */}
              <div className="flex items-center gap-3">
                <div className="flex-1 border-t border-zinc-100" />
                <span className="text-xs text-zinc-400">o</span>
                <div className="flex-1 border-t border-zinc-100" />
              </div>

              {/* Confirmar sin URL */}
              <div className="space-y-2">
                <p className="text-xs text-zinc-400">
                  Confirma el contenido actual sin necesidad de URL de CDL.
                </p>
                <button
                  type="button"
                  onClick={handleConfirmWithoutCdl}
                  disabled={isPending}
                  className="w-full rounded-lg border border-zinc-200 px-4 py-2 text-sm text-zinc-700 hover:bg-zinc-50 disabled:opacity-50 transition-colors"
                >
                  {isPending ? "Confirmando…" : "Confirmar sin CDL"}
                </button>
              </div>

              {error && <p className="text-sm text-red-600">{error}</p>}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
