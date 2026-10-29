"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  applyKost2Reallocation,
  deleteKost2ReallocationRun,
  previewKost2Reallocation,
  reapplyKost2Reallocation,
  revertKost2Reallocation,
  saveKost2ReallocationRun,
  type Kost2ReallocationPlan,
  type Kost2ReallocationResult,
  type Kost2ReallocationRun,
} from "@/lib/rs/kost2-reallocation";
import { KOST2_REALLOCATION_RUNS_KEY } from "./kost2-reallocation-list";
import type { RunDraft } from "./types";

export const runKey = (id: number | null) => ["kost2Reallocation", "run", id];

/**
 * The actions of the wizard on one run. Every action but the preview first saves the draft, so the server always
 * calculates from what is on screen (it takes the run from its database, never from the request). A refused
 * action — HTTP 406 — leaves its reason in `error`; nothing was written then. The plan (preview or result) is kept
 * by the caller, above the remount of the form after a save.
 */
export function useRunActions(
  runId: number | null,
  draft: RunDraft,
  setPlan: (plan: Kost2ReallocationPlan | null) => void
) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [error, setError] = useState<string | null>(null);

  const body = (): Kost2ReallocationRun => ({
    ...draft,
    id: runId,
    needsReapply: false,
    log: [],
  });
  const refused = <T>(
    res: Kost2ReallocationResult<T>
  ): res is Extract<typeof res, { kind: "refused" }> => {
    if (res.kind === "ok") {
      setError(null);
      return false;
    }
    setError(res.validationErrors.map((e) => e.message).join(" "));
    return true;
  };
  const stored = (run: Kost2ReallocationRun) => {
    queryClient.setQueryData(runKey(run.id ?? null), run);
    queryClient.invalidateQueries({ queryKey: KOST2_REALLOCATION_RUNS_KEY });
    if (runId == null) router.replace(`/kost2-reallocation/${run.id}`);
  };
  /** Saves the draft, then the action with the saved run's id. */
  const afterSave = async <T>(
    action: (id: number) => Promise<Kost2ReallocationResult<T>>
  ) => {
    const saved = await saveKost2ReallocationRun(body());
    if (refused(saved)) return null;
    const res = await action(saved.value.id!);
    if (refused(res)) return null;
    return { run: saved.value, value: res.value };
  };

  const save = useMutation({
    mutationFn: () => saveKost2ReallocationRun(body()),
    onSuccess: (res) => !refused(res) && stored(res.value),
  });
  const preview = useMutation({
    mutationFn: () => afterSave((id) => previewKost2Reallocation(id)),
    onSuccess: (r) => r && (setPlan(r.value), stored(r.run)),
  });
  const apply = useMutation({
    mutationFn: () => afterSave((id) => applyKost2Reallocation(id)),
    onSuccess: async (r) => {
      if (!r) return;
      setPlan(r.value);
      queryClient.invalidateQueries({ queryKey: KOST2_REALLOCATION_RUNS_KEY });
      // Refetched (the status changed), which remounts the wizard read-only; a new run moves to its own url.
      if (runId == null) router.replace(`/kost2-reallocation/${r.run.id}`);
      else await queryClient.invalidateQueries({ queryKey: runKey(runId) });
    },
  });
  const reapply = useMutation({
    mutationFn: () => reapplyKost2Reallocation(runId!),
    onSuccess: (res) => {
      if (refused(res)) return;
      setPlan(res.value);
      queryClient.invalidateQueries({ queryKey: runKey(runId) });
    },
  });
  const revert = useMutation({
    mutationFn: () => revertKost2Reallocation(runId!),
    onSuccess: (res) => {
      if (refused(res)) return;
      setPlan(null);
      stored(res.value);
    },
  });
  const remove = useMutation({
    mutationFn: () => deleteKost2ReallocationRun(runId!),
    onSuccess: (res) => {
      if (refused(res)) return;
      queryClient.invalidateQueries({ queryKey: KOST2_REALLOCATION_RUNS_KEY });
      router.replace("/kost2-reallocation");
    },
  });
  const pending = [save, preview, apply, reapply, revert, remove].some(
    (m) => m.isPending
  );
  return {
    save,
    preview,
    apply,
    reapply,
    revert,
    remove,
    error,
    pending,
  };
}
