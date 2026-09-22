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
import { selectableIds } from "./import-model";
import type { ImportConfig, ImportView } from "./import-types";

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
        setView(result.view);
      } else {
        toast.error(result.error);
      }
    },
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  const reconcile = useMutation({
    mutationFn: () => reconcileImport(base),
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
    uploadProgress,
    upload,
    reconcile,
    commit,
    cancel,
    t,
  };
}
