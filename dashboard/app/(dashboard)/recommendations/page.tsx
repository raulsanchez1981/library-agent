export default function RecommendationsPage() {
  return (
    <div className="px-8 py-8">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-zinc-900">Recomendaciones</h1>
        <p className="mt-1 text-sm text-zinc-500">
          Libros seleccionados para ti por Claude según tu perfil lector
        </p>
      </div>

      {/* Placeholder — se conecta a la API en 4.5.3 */}
      <div className="rounded-xl border border-dashed border-zinc-200 bg-white p-12 text-center">
        <span className="text-4xl">📖</span>
        <p className="mt-4 text-sm font-medium text-zinc-500">
          Las recomendaciones aparecerán aquí
        </p>
        <p className="mt-1 text-xs text-zinc-400">
          Ejecuta un trigger de scoring para generar recomendaciones
        </p>
      </div>
    </div>
  );
}
