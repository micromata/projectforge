"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  appendHistoryComment,
  fetchHistory,
  type HistoryInfo,
} from "@/lib/rs/history";

/** Query key of an entity's history, also used to invalidate it after a save. */
export function historyQueryKey(entity: string, id: number | null) {
  return ["history", entity, id] as const;
}

/**
 * The change history of one entity. Mount the component using this only when the history is on
 * screen — the query fires as soon as it does, and a long history is expensive to build.
 */
export function useHistory(entity: string, id: number) {
  return useQuery<HistoryInfo>({
    queryKey: historyQueryKey(entity, id),
    queryFn: ({ signal }) => fetchHistory(entity, id, signal),
    enabled: Number.isFinite(id) && id > 0,
  });
}

/**
 * Appends a comment to one of the entity's history entries. Comments are append-only, so the
 * history is re-read rather than patched locally.
 */
export function useAppendHistoryComment(entity: string, id: number) {
  const qc = useQueryClient();
  return useMutation<void, Error, { entryId: number; comment: string }>({
    mutationFn: ({ entryId, comment }) =>
      appendHistoryComment(entryId, comment),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: historyQueryKey(entity, id) });
    },
  });
}
