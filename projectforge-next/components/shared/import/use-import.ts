"use client";

import { useCallback, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { RowSelectionState } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { toast } from "@/lib/toast";
import {
  cancelImport,
  commitImport,
  fetchImportState,
  reconcileImport,
  uploadImportFile,
} from "@/lib/rs/import";
import type { UploadProgress } from "@/lib/rs/upload";
import { useJobStore } from "@/store/job-store";
import {
  filterEntriesByStatus,
  selectableIds,
  STATUS_GROUP_OF,
} from "./import-model";
import type { ImportConfig, ImportView } from "./import-types";

/**
 * The reconcile always asks the backend for every status — including the unmodified rows the default
 * options would drop — so the whole row set is on the client and the status chips can show/hide it
 * without a server round-trip. The aggregate counts are computed over all rows regardless (see
 * `AbstractImportRest.buildView`), so this does not change them.
 */
const ALL_STATUSES = {
  new: true,
  modified: true,
  unmodified: true,
  imported: true,
  deleted: true,
  faulty: true,
  unknown: true,
} as const;

/** Hidden by default, so the unmodified rows we now fetch stay out of sight until the chip is toggled. */
const DEFAULT_HIDDEN_STATUS_KEYS = ["unmodified"] as const;

/**
 * The whole state of one import route: the current [ImportView] (React-Query owned, so a reconcile or a
 * fresh upload refreshes it), the ticked row ids, the display options, and the four mutations. On a
 * successful commit the returned job id is handed to the job store — whose toast is mounted app-wide and
 * survives the navigation — and the user is sent back to the entity's list.
 */
export function useImport(config: ImportConfig) {
  const base = config.endpoints.base;
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();
  const watchJob = useJobStore((s) => s.watchJob);

  const stateKey = useMemo(() => ["import", base, "state"] as const, [base]);
  const [selection, setSelection] = useState<RowSelectionState>({});
  const [hiddenStatusKeys, setHiddenStatusKeys] = useState<Set<string>>(
    () => new Set(DEFAULT_HIDDEN_STATUS_KEYS)
  );
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);

  const query = useQuery({
    queryKey: stateKey,
    queryFn: ({ signal }) => fetchImportState(base, signal),
    // The stash lives in the session; a background refetch would only ever re-read what this page owns.
    refetchOnWindowFocus: false,
  });

  const setView = useCallback(
    (view: ImportView) => queryClient.setQueryData(stateKey, view),
    [queryClient, stateKey]
  );

  const upload = useMutation({
    mutationFn: (file: File) => {
      setUploadProgress(0);
      return uploadImportFile(base, file, {
        onProgress: (p: UploadProgress) => setUploadProgress(p.percent ?? 0),
      });
    },
    onSettled: () => setUploadProgress(null),
    onSuccess: (result) => {
      if (result.kind === "ok") {
        setSelection({});
        setHiddenStatusKeys(new Set(DEFAULT_HIDDEN_STATUS_KEYS));
        setView(result.view);
      } else {
        toast.error(result.error);
      }
    },
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  const reconcile = useMutation({
    mutationFn: () => reconcileImport(base, ALL_STATUSES),
    onSuccess: (view) => setView(view),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  const commit = useMutation({
    mutationFn: (selectedIds: number[]) => commitImport(base, selectedIds),
    onSuccess: ({ jobId }) => {
      watchJob(jobId);
      queryClient.removeQueries({ queryKey: stateKey });
      router.push(config.returnRoute);
    },
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  const cancel = useMutation({
    mutationFn: () => cancelImport(base),
    onSuccess: () => {
      setSelection({});
      setHiddenStatusKeys(new Set(DEFAULT_HIDDEN_STATUS_KEYS));
      setView({ hasBeenReconciled: false, entries: [] });
    },
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  const view = query.data;
  const selectedIds = useMemo(
    () =>
      Object.keys(selection)
        .filter((id) => selection[id])
        .map(Number)
        .filter((id) => !Number.isNaN(id)),
    [selection]
  );

  const selectAll = useCallback(() => {
    const ids = selectableIds(view?.entries ?? [], config.selectableStatuses);
    setSelection(Object.fromEntries(ids.map((id) => [String(id), true])));
  }, [view, config.selectableStatuses]);

  const filteredEntries = useMemo(
    () => filterEntriesByStatus(view?.entries ?? [], hiddenStatusKeys),
    [view, hiddenStatusKeys]
  );

  /**
   * Show/hide a status group. When a group is newly hidden, its rows are dropped from the selection too —
   * otherwise a hidden row could still be committed, which the user can no longer see to deselect.
   */
  const toggleStatusKey = useCallback(
    (key: string) => {
      setHiddenStatusKeys((previous) => {
        const next = new Set(previous);
        if (next.has(key)) {
          next.delete(key);
        } else {
          next.add(key);
          const hiddenIds = new Set(
            (view?.entries ?? [])
              .filter((entry) => STATUS_GROUP_OF[entry.status] === key)
              .map((entry) => String(entry.id))
          );
          if (hiddenIds.size > 0) {
            setSelection((current) => {
              const pruned = Object.fromEntries(
                Object.entries(current).filter(([id]) => !hiddenIds.has(id))
              );
              return pruned;
            });
          }
        }
        return next;
      });
    },
    [view]
  );

  return {
    query,
    view,
    /** True once a file has been uploaded (the view carries a filename/info). */
    hasStorage: Boolean(view?.filename || view?.info),
    selection,
    setSelection,
    selectedIds,
    selectAll,
    clearSelection: () => setSelection({}),
    hiddenStatusKeys,
    toggleStatusKey,
    filteredEntries,
    uploadProgress,
    upload,
    reconcile,
    commit,
    cancel,
    t,
  };
}
