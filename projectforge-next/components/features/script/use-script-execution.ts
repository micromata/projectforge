"use client";

import { useEffect } from "react";
import { useForm } from "@tanstack/react-form";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { RsError } from "@/lib/rs/client";
import {
  executeScript,
  fetchScriptLog,
  loadScriptExecution,
  scriptExecuteQueryKey,
  type Script,
  type ScriptExecuteEndpoint,
} from "@/lib/rs/script";
import { toast } from "@/lib/toast";

/** How often the log is fetched while the script runs. */
const LOG_POLL_MS = 2000;

/**
 * The execution of a script: the form (the parameter values, or the code of an ad-hoc execution), the
 * execution itself and its log.
 *
 * The log is polled while the script runs and fetched once more when it has finished, so its last lines
 * arrive too. The backend keeps the user's running or last one for a few minutes, with its outcome, so a
 * user coming back sees them again.
 *
 * @param id The stored script, or null for ad-hoc code (administration only).
 * @param example The example the ad-hoc editor starts with.
 * @param from The page target the script was started from by its button (see ScriptPageButtons): the
 *   script gets the current filter of that page.
 * @param onExecute Called when an execution starts (the page shows its output then, the log polled
 *   while it runs).
 */
export function useScriptExecution(
  endpoint: ScriptExecuteEndpoint,
  id: number | null,
  example: number | null,
  from: string | null = null,
  onExecute?: () => void
) {
  const queryClient = useQueryClient();
  const queryKey = scriptExecuteQueryKey(endpoint, id, example, from);
  const load = useQuery({
    queryKey,
    queryFn: ({ signal }) =>
      loadScriptExecution(endpoint, id, example, from, signal),
    // Refetched, the form would be reset onto the stored values and lose what the user entered.
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });
  const logKey = [...queryKey, "log"] as const;

  const execution = useMutation({
    mutationFn: (script: Script) =>
      executeScript(endpoint, { ...script, pageTarget: from }),
    onError: (error) =>
      toast.error(error instanceof RsError ? error.message : String(error)),
    onMutate: () => onExecute?.(),
    onSettled: () => queryClient.invalidateQueries({ queryKey: logKey }),
  });

  const log = useQuery({
    queryKey: logKey,
    queryFn: ({ signal }) => fetchScriptLog(endpoint, id, signal),
    enabled: !execution.isIdle || !!load.data?.lastExecution,
    refetchInterval: execution.isPending ? LOG_POLL_MS : false,
  });

  const form = useForm({
    defaultValues: load.data?.script ?? ({ id } as Script),
    onSubmit: async ({ value }) => {
      await execution.mutateAsync(value as Script).catch(() => undefined);
    },
  });

  useEffect(() => {
    if (load.data) form.reset(load.data.script);
  }, [load.data, form]);

  return {
    form,
    load,
    execution,
    log,
    /** The outcome of this execution, or before one the user's last one of this script. */
    result: execution.isIdle
      ? (load.data?.lastExecution ?? undefined)
      : execution.data,
    /** The file of this execution, or else of the user's last one. */
    download: execution.data?.download ?? load.data?.download ?? null,
  };
}
