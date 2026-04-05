"use client";

import { useTransition, useState } from "react";
import { updateProfile } from "@/app/actions/profile";
import type { UserProfileDto } from "@/lib/types";

const LANGUAGE_LABELS: Record<string, string> = {
  es: "Español",
  en: "Inglés",
};

function parseList(raw: string): string[] {
  return raw
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

export default function ProfileForm({ profile }: { profile: UserProfileDto }) {
  const [isPending, startTransition] = useTransition();
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [language, setLanguage] = useState(profile.preferredLanguage ?? "es");
  const [threshold, setThreshold] = useState(
    profile.minScoreThreshold != null
      ? Math.round(profile.minScoreThreshold * 100)
      : 70
  );
  const [genresRaw, setGenresRaw] = useState(
    profile.favoriteGenres.join(", ")
  );
  const [authorsRaw, setAuthorsRaw] = useState(
    profile.favoriteAuthors.join(", ")
  );

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSuccess(false);
    setError(null);

    startTransition(async () => {
      try {
        await updateProfile({
          preferredLanguage: language,
          minScoreThreshold: threshold / 100,
          favoriteGenres: parseList(genresRaw),
          favoriteAuthors: parseList(authorsRaw),
        });
        setSuccess(true);
      } catch {
        setError("No se pudo guardar el perfil. Inténtalo de nuevo.");
      }
    });
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {/* Idioma preferido */}
      <div>
        <label className="block text-sm font-medium text-zinc-700 mb-1">
          Idioma preferido
        </label>
        <select
          value={language}
          onChange={(e) => setLanguage(e.target.value)}
          className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-900"
        >
          {Object.entries(LANGUAGE_LABELS).map(([code, label]) => (
            <option key={code} value={code}>
              {label}
            </option>
          ))}
        </select>
      </div>

      {/* Umbral mínimo de score */}
      <div>
        <label className="block text-sm font-medium text-zinc-700 mb-1">
          Umbral mínimo de score:{" "}
          <span className="font-semibold text-zinc-900">{threshold}</span>
        </label>
        <input
          type="range"
          min={0}
          max={100}
          value={threshold}
          onChange={(e) => setThreshold(Number(e.target.value))}
          className="w-full accent-zinc-900"
        />
        <div className="flex justify-between text-xs text-zinc-400 mt-1">
          <span>0 — todo</span>
          <span>100 — solo lo mejor</span>
        </div>
      </div>

      {/* Géneros favoritos */}
      <div>
        <label className="block text-sm font-medium text-zinc-700 mb-1">
          Géneros favoritos
        </label>
        <input
          type="text"
          value={genresRaw}
          onChange={(e) => setGenresRaw(e.target.value)}
          placeholder="Fantasía, Ciencia ficción, Terror..."
          className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-900"
        />
        <p className="mt-1 text-xs text-zinc-400">Separados por comas</p>
      </div>

      {/* Autores favoritos */}
      <div>
        <label className="block text-sm font-medium text-zinc-700 mb-1">
          Autores favoritos
        </label>
        <input
          type="text"
          value={authorsRaw}
          onChange={(e) => setAuthorsRaw(e.target.value)}
          placeholder="Brandon Sanderson, Patrick Rothfuss..."
          className="w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-900"
        />
        <p className="mt-1 text-xs text-zinc-400">Separados por comas</p>
      </div>

      {/* Feedback */}
      {success && (
        <p className="rounded-lg bg-emerald-50 border border-emerald-100 px-3 py-2 text-sm text-emerald-700">
          Perfil actualizado correctamente.
        </p>
      )}
      {error && (
        <p className="rounded-lg bg-red-50 border border-red-100 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={isPending}
        className="rounded-lg bg-zinc-900 px-4 py-2 text-sm font-medium text-white hover:bg-zinc-800 disabled:opacity-50 transition-colors"
      >
        {isPending ? "Guardando…" : "Guardar cambios"}
      </button>
    </form>
  );
}
