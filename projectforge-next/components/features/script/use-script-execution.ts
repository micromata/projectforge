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
 * arrive too. Before the first execution there is none to show: the backend only keeps the user's
 * running or last one.
 *
 * @param id The stored script, or null for ad-hoc code (administration only).
 * @param example The example the ad-hoc editor starts with.
 */
export function useScriptExecution(
  endpoint: ScriptExecuteEndpoint,
  id: number | null,
  example: number | null
) {
  const queryClient = useQueryClient();
  const queryKey = scriptExecuteQueryKey(endpoint, id, example);
  const load = useQuery({
    queryKey,
    queryFn: ({ signal }) => loadScriptExecution(endpoint, id, example, signal),
    // Refetched, the form would be reset onto the stored values and lose what the user entered.
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });
  const logKey = [...queryKey, "log"] as const;

  const execution = useMutation({
    mutationFn: (script: Script) => executeScript(endpoint, script),
    onError: (error) =>
      toast.error(error instanceof RsError ? error.message : String(error)),
    onSettled: () => queryClient.invalidateQueries({ queryKey: logKey }),
  });

  const log = useQuery({
    queryKey: logKey,
    queryFn: ({ signal }) => fetchScriptLog(endpoint, id, signal),
    enabled: !execution.isIdle,
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
    /** The file of this execution, or else of the user's last one. */
    download: execution.data?.download ?? load.data?.download ?? null,
  };
}
