import { apiFetch } from "@/lib/api";
import HistoryList from "@/components/history/history-list";
import AddHistoryForm from "@/components/history/add-history-form";
import type { ApiResponse, ReadingHistoryDto } from "@/lib/types";

async function fetchHistory(): Promise<ReadingHistoryDto[]> {
  const res = await apiFetch("/api/v1/reading-history", { cache: "no-store" });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`HTTP ${res.status} — ${body || res.statusText}`);
  }
  const json: ApiResponse<ReadingHistoryDto[]> = await res.json();
  return json.data;
}

export default async function HistoryPage() {
  const entries = await fetchHistory();

  return (
    <div className="px-8 py-8">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-zinc-900">Historial de lectura</h1>
          <p className="mt-1 text-sm text-zinc-500">
            {entries.length > 0
              ? `${entries.length} libro${entries.length !== 1 ? "s" : ""} registrado${entries.length !== 1 ? "s" : ""}`
              : "Libros leídos, en curso y pendientes"}
          </p>
        </div>
      </div>

      <div className="space-y-4">
        <AddHistoryForm />
        <HistoryList entries={entries} />
      </div>
    </div>
  );
}
