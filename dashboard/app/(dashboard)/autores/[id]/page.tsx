import { fetchAutorDetail } from "@/app/actions/autores";
import Link from "next/link";
import Image from "next/image";
import type { AutorBookDto } from "@/lib/types";
import BookCoverImage from "@/components/biblioteca/book-cover-image";

export default async function AutorDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const autor = await fetchAutorDetail(id);

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

      {/* Cabecera del autor */}
      <div className="flex gap-8 mb-10">
        {/* Foto */}
        <div className="flex-shrink-0">
          <div className="relative w-32 h-32 rounded-full overflow-hidden bg-zinc-100 ring-1 ring-zinc-200 shadow-sm">
            {autor.photoUrl ? (
              <Image
                src={autor.photoUrl}
                alt={autor.name}
                fill
                className="object-cover"
                sizes="128px"
              />
            ) : (
              <div className="flex h-full items-center justify-center">
                <svg className="w-12 h-12 text-zinc-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                    d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                </svg>
              </div>
            )}
          </div>
        </div>

        {/* Nombre y bio */}
        <div className="flex-1 min-w-0">
          <h1 className="text-2xl font-semibold text-zinc-900">{autor.name}</h1>
          <p className="mt-1 text-sm text-zinc-400">
            {autor.books.length} libro{autor.books.length !== 1 ? "s" : ""} en tu biblioteca
          </p>
          {autor.bio && (
            <p className="mt-3 text-sm text-zinc-600 leading-relaxed">{autor.bio}</p>
          )}
          {autor.openLibraryOlid && (
            <a
              href={`https://openlibrary.org/authors/${autor.openLibraryOlid}`}
              target="_blank"
              rel="noopener noreferrer"
              className="mt-3 inline-flex items-center gap-1 text-xs text-zinc-400 hover:text-zinc-700 transition-colors"
            >
              Ver en Open Library
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          )}
        </div>
      </div>

      {/* Listado de libros */}
      {autor.books.length > 0 && (
        <div>
          <h2 className="text-xs font-semibold uppercase tracking-wide text-zinc-400 mb-4">
            En tu biblioteca
          </h2>
          <div className="grid grid-cols-3 gap-5 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6">
            {autor.books.map((book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function BookCard({ book }: { book: AutorBookDto }) {
  return (
    <Link href={`/biblioteca/${book.id}`} className="group flex flex-col gap-2">
      <div className="relative aspect-[2/3] w-full overflow-hidden rounded-lg bg-zinc-100 shadow-sm ring-1 ring-zinc-200 transition-shadow group-hover:shadow-md">
        {book.coverUrl ? (
          <BookCoverImage src={book.coverUrl} alt={book.name} />
        ) : (
          <div className="flex h-full items-center justify-center p-3">
            <span className="text-center text-xs text-zinc-400 leading-snug">{book.name}</span>
          </div>
        )}
      </div>
      <p className="text-xs font-medium text-zinc-800 leading-snug line-clamp-2" title={book.name}>
        {book.name}
      </p>
    </Link>
  );
}
