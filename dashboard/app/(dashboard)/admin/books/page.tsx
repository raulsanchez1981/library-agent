import { fetchAdminBooks } from "@/app/actions/admin-books";
import BooksAdminClient from "@/components/admin/books-admin-client";

export default async function AdminBooksPage({
  searchParams,
}: {
  searchParams: Promise<{
    search?: string;
    confidence?: string;
    enriched?: string;
    sortBy?: string;
    sortDir?: string;
    page?: string;
  }>;
}) {
  const {
    search,
    confidence,
    enriched,
    sortBy,
    sortDir,
    page: pageParam,
  } = await searchParams;
  const page = Math.max(0, Number(pageParam ?? 0));

  const data = await fetchAdminBooks({ search, confidence, enriched, sortBy, sortDir, page });

  return (
    <div className="px-8 py-8">
      <div className="mb-6 flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Libros ingestados</h1>
          <p className="mt-1 text-sm text-zinc-500">
            {data.totalElements > 0
              ? `${data.totalElements} libro${data.totalElements !== 1 ? "s" : ""} en la base de datos`
              : "Sin libros ingestados todavía"}
          </p>
        </div>
      </div>

      <BooksAdminClient
        data={data}
        currentFilters={{ search, confidence, enriched }}
        currentSort={{ sortBy: sortBy ?? "createdAt", sortDir: sortDir ?? "desc" }}
        currentPage={page}
      />
    </div>
  );
}
