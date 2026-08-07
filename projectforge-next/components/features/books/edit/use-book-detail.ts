"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchOne } from "@/lib/rs/client";
import {
  markEntityAsDeleted,
  saveOrUpdateEntity,
  type EntityWriteResult,
} from "@/lib/rs/entity";
import { historyQueryKey } from "@/hooks/use-history";
import type { BookDetail } from "../types";

const ENTITY = "book";

/** id null = a book being added, which has nothing to load yet. */
export function useBookDetail(id: number | null) {
  return useQuery<BookDetail>({
    queryKey: [ENTITY, id],
    queryFn: ({ signal }) => fetchOne<BookDetail>(ENTITY, id!, signal),
    enabled: id != null && Number.isFinite(id) && id > 0,
  });
}

/**
 * Saves a book — insert and update alike, the backend tells them apart by `data.id`.
 *
 * The answer carries no entity, only the id (see lib/rs/entity.ts), so the caches are invalidated
 * instead of written: the saved book comes back from the server on the next read, including the
 * fields it computed itself.
 */
export function useSaveBook() {
  const qc = useQueryClient();
  return useMutation<EntityWriteResult, Error, BookDetail>({
    mutationFn: (book) => saveOrUpdateEntity(ENTITY, book),
    onSuccess: (result, book) => {
      // A rejected entity is a regular answer here (HTTP 406), not an error - nothing changed.
      if (result.kind !== "ok") return;
      invalidate(qc, result.id ?? book.id);
    },
  });
}

/**
 * Marks a book as deleted. This is the delete a historized entity offers: the row survives and
 * `RestPaths.UNDELETE` can bring it back.
 */
export function useDeleteBook() {
  const qc = useQueryClient();
  return useMutation<EntityWriteResult, Error, BookDetail>({
    mutationFn: (book) => markEntityAsDeleted(ENTITY, book),
    onSuccess: (result, book) => {
      if (result.kind !== "ok") return;
      invalidate(qc, book.id);
    },
  });
}

function invalidate(
  qc: ReturnType<typeof useQueryClient>,
  id: number | null
): void {
  void qc.invalidateQueries({ queryKey: ["books"] });
  if (id == null) return;
  void qc.invalidateQueries({ queryKey: [ENTITY, id] });
  // Every save writes a history entry, so the timeline of this book is stale too.
  void qc.invalidateQueries({ queryKey: historyQueryKey(ENTITY, id) });
}
