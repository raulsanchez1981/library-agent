export default function HistoryPage() {
  return (
    <div className="px-8 py-8">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-zinc-900">Historial de lectura</h1>
        <p className="mt-1 text-sm text-zinc-500">
          Libros leídos, en curso y pendientes
        </p>
      </div>

      {/* Placeholder — se conecta a la API en 4.5.4 */}
      <div className="rounded-xl border border-dashed border-zinc-200 bg-white p-12 text-center">
        <span className="text-4xl">📚</span>
        <p className="mt-4 text-sm font-medium text-zinc-500">
          Tu historial de lectura aparecerá aquí
        </p>
      </div>
    </div>
  );
}
