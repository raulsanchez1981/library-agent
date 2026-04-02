import { fetchBiblioteca } from "@/app/actions/biblioteca";
import type { VerifiedTitleDto } from "@/lib/types";
import Image from "next/image";
import Link from "next/link";

export default async function BibliotecaPage() {
  const books = await fetchBiblioteca();

  return (
    <div className="px-8 py-8">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-zinc-900">Biblioteca</h1>
        <p className="mt-1 text-sm text-zinc-500">
          {books.length > 0
            ? `${books.length} libro${books.length !== 1 ? "s" : ""} verificados`
            : "Sin libros verificados todavía"}
        </p>
      </div>

      {books.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-zinc-400">
          <p className="text-sm">Verifica libros desde el panel de administración para verlos aquí.</p>
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
          {books.map((book) => (
            <BookCard key={book.id} book={book} />
          ))}
        </div>
      )}
    </div>
  );
}

function BookCard({ book }: { book: VerifiedTitleDto }) {
  return (
    <Link href={`/biblioteca/${book.id}`} className="group flex flex-col gap-2">
      {/* Portada */}
      <div className="relative aspect-[2/3] w-full overflow-hidden rounded-lg bg-zinc-100 shadow-sm ring-1 ring-zinc-200 transition-shadow group-hover:shadow-md">
        {book.coverUrl ? (
          <Image
            src={book.coverUrl}
            alt={book.name}
            fill
            className="object-cover"
            sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 16vw"
          />
        ) : (
          <div className="flex h-full items-center justify-center p-3">
            <span className="text-center text-xs text-zinc-400 leading-snug">{book.name}</span>
          </div>
        )}
      </div>

      {/* Info */}
      <div>
        <p className="text-sm font-medium text-zinc-900 leading-snug line-clamp-2" title={book.name}>
          {book.name}
        </p>
        {book.authors.length > 0 && (
          <p className="mt-0.5 text-xs text-zinc-500 truncate" title={book.authors.join(", ")}>
            {book.authors.join(", ")}
          </p>
        )}
        {book.synopsis && (
          <p className="mt-1 text-xs text-zinc-400 line-clamp-3 leading-relaxed">
            {book.synopsis}
          </p>
        )}
      </div>
    </Link>
  );
}
