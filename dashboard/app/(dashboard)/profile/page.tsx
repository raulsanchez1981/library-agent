export default function ProfilePage() {
  return (
    <div className="px-8 py-8">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-zinc-900">Mi perfil lector</h1>
        <p className="mt-1 text-sm text-zinc-500">
          Tus preferencias de lectura — el motor de recomendaciones las usa para puntuarte libros
        </p>
      </div>

      {/* Placeholder — se conecta a la API en 4.5.4 */}
      <div className="rounded-xl border border-dashed border-zinc-200 bg-white p-12 text-center">
        <span className="text-4xl">👤</span>
        <p className="mt-4 text-sm font-medium text-zinc-500">
          Formulario de preferencias próximamente
        </p>
      </div>
    </div>
  );
}
