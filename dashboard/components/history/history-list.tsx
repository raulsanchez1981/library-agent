"use client";

import { useOptimistic, useTransition, useState } from "react";
import { updateHistoryEntry, deleteHistoryEntry } from "@/app/actions/history";
import type { ReadingHistoryDto, ReadingStatus } from "@/lib/types";

const STATUS_LABELS: Record<ReadingStatus, string> = {
  PENDING: "Pendiente",
  IN_PROGRESS: "En curso",
  READ: "Leído",
  ABANDONED: "Abandonado",
};

const STATUS_STYLES: Record<ReadingStatus, string> = {
  PENDING: "bg-zinc-100 text-zinc-600",
  IN_PROGRESS: "bg-amber-100 text-amber-700",
  READ: "bg-emerald-100 text-emerald-700",
  ABANDONED: "bg-red-100 text-red-600",
};

const NEXT_STATUS: Record<ReadingStatus, ReadingStatus> = {
  PENDING: "IN_PROGRESS",
  IN_PROGRESS: "READ",
  READ: "ABANDONED",
  ABANDONED: "PENDING",
};

function StatusBadge({ status }: { status: ReadingStatus }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[status]}`}
    >
      {STATUS_LABELS[status]}
    </span>
  );
}

function HistoryItem({
  entry,
  onDelete,
}: {
  entry: ReadingHistoryDto;
  onDelete: (id: string) => void;
}) {
  const [isPending, startTransition] = useTransition();
  const [optimisticStatus, setOptimisticStatus] = useOptimistic(entry.status);
  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState(entry.bookTitle);
  const [editAuthor, setEditAuthor] = useState(entry.bookAuthor ?? "");

  function handleStatusChange(next: ReadingStatus) {
    startTransition(async () => {
      setOptimisticStatus(next);
      await updateHistoryEntry(entry.id, { status: next });
    });
  }

  function handleDelete() {
    startTransition(async () => {
      onDelete(entry.id);
      await deleteHistoryEntry(entry.id);
    });
  }

  function handleEditSave() {
    if (!editTitle.trim()) return;
    startTransition(async () => {
      await updateHistoryEntry(entry.id, {
        bookTitle: editTitle.trim(),
        bookAuthor: editAuthor.trim() || null,
      });
      setEditing(false);
    });
  }

  if (editing) {
    return (
      <li className="rounded-xl border border-zinc-300 bg-white p-4 space-y-3">
        <div className="flex gap-3">
          <input
            type="text"
            value={editTitle}
            onChange={(e) => setEditTitle(e.target.value)}
            placeholder="Título *"
            className="flex-1 rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-900"
          />
          <input
            type="text"
            value={editAuthor}
            onChange={(e) => setEditAuthor(e.target.value)}
            placeholder="Autor (opcional)"
            className="flex-1 rounded-lg border border-zinc-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-zinc-900"
          />
        </div>
        <div className="flex gap-2">
          <button
            onClick={handleEditSave}
            disabled={isPending || !editTitle.trim()}
            className="rounded-lg bg-zinc-900 px-3 py-1.5 text-xs font-medium text-white hover:bg-zinc-800 disabled:opacity-50 transition-colors"
          >
            {isPending ? "Guardando…" : "Guardar"}
          </button>
          <button
            onClick={() => {
              setEditing(false);
              setEditTitle(entry.bookTitle);
              setEditAuthor(entry.bookAuthor ?? "");
            }}
            className="rounded-lg border border-zinc-200 px-3 py-1.5 text-xs text-zinc-600 hover:bg-zinc-50 transition-colors"
          >
            Cancelar
          </button>
        </div>
      </li>
    );
  }

  return (
    <li className="flex items-start gap-4 rounded-xl border border-zinc-100 bg-white p-4">
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-zinc-900 truncate">
          {entry.bookTitle}
        </p>
        {entry.bookAuthor && (
          <p className="text-xs text-zinc-500 mt-0.5">{entry.bookAuthor}</p>
        )}
        {entry.rating != null && (
          <p className="text-xs text-zinc-400 mt-1">
            {"★".repeat(entry.rating)}
            {"☆".repeat(5 - entry.rating)}
          </p>
        )}
      </div>

      <div className="flex flex-col items-end gap-2 shrink-0">
        <StatusBadge status={optimisticStatus} />
        <button
          onClick={() => handleStatusChange(NEXT_STATUS[optimisticStatus])}
          disabled={isPending}
          className="text-xs text-zinc-400 hover:text-zinc-700 disabled:opacity-40 transition-colors"
        >
          → {STATUS_LABELS[NEXT_STATUS[optimisticStatus]]}
        </button>
        <div className="flex gap-3 mt-1">
          <button
            onClick={() => setEditing(true)}
            className="text-xs text-zinc-400 hover:text-zinc-700 transition-colors"
          >
            Editar
          </button>
          <button
            onClick={handleDelete}
            disabled={isPending}
            className="text-xs text-red-400 hover:text-red-600 disabled:opacity-40 transition-colors"
          >
            Eliminar
          </button>
        </div>
      </div>
    </li>
  );
}

export default function HistoryList({
  entries,
}: {
  entries: ReadingHistoryDto[];
}) {
  const [optimisticEntries, removeEntry] = useOptimistic(
    entries,
    (state, id: string) => state.filter((e) => e.id !== id)
  );

  if (optimisticEntries.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-zinc-200 bg-white p-12 text-center">
        <span className="text-4xl">📖</span>
        <p className="mt-4 text-sm font-medium text-zinc-500">
          Aún no has añadido ningún libro
        </p>
      </div>
    );
  }

  return (
    <ul className="space-y-3">
      {optimisticEntries.map((entry) => (
        <HistoryItem key={entry.id} entry={entry} onDelete={removeEntry} />
      ))}
    </ul>
  );
}
