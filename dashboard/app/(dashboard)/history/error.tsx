"use client";

export default function HistoryError({ error }: { error: Error }) {
  return (
    <div className="px-8 py-8">
      <div className="rounded-xl border border-red-100 bg-red-50 p-6">
        <p className="text-sm font-medium text-red-800">Error al cargar el historial</p>
        <p className="mt-1 font-mono text-xs text-red-600">{error.message}</p>
      </div>
    </div>
  );
}
