"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { FormAlert } from "@/components/shared/form-alert";
import { TaskSelect } from "@/components/shared/tasks/task-select";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  fetchKost2ReallocationTimesheets,
  type Kost2ReallocationTimesheets,
} from "@/lib/rs/kost2-reallocation";
import { TimesheetsTable } from "./timesheets-table";

/**
 * Step 2: the time sheets the run moves. The detected ones (their task is in the task tree of the target project, but
 * their cost unit isn't) are moved with the run; time sheets booked directly on a collective task are searched
 * and ticked by hand — they also get the target task of the run.
 */
export function TimesheetsStep({
  runId,
  manualIds,
  onManualIdsChange,
  readOnly,
}: {
  runId: number;
  manualIds: Set<number>;
  onManualIdsChange: (ids: Set<number>) => void;
  readOnly: boolean;
}) {
  const t = useTranslations();
  const [taskId, setTaskId] = useState<number | null>(null);
  const [text, setText] = useState("");
  const [result, setResult] = useState<Kost2ReallocationTimesheets | null>(
    null
  );
  const [error, setError] = useState<string | null>(null);

  const load = useMutation({
    mutationFn: async (searchTaskId: number | null) => {
      const res = await fetchKost2ReallocationTimesheets({
        runId,
        taskIds: searchTaskId ? [searchTaskId] : [],
        text,
      });
      if (res.kind === "refused") {
        setError(res.validationErrors.map((e) => e.message).join(" "));
        return;
      }
      setError(null);
      setResult(res.value);
    },
  });

  const toggle = (id: number, checked: boolean) => {
    const next = new Set(manualIds);
    if (checked) next.add(id);
    else next.delete(id);
    onManualIdsChange(next);
  };

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-2">
        <Button
          type="button"
          variant="outline"
          disabled={load.isPending}
          onClick={() => load.mutate(null)}
        >
          {t("fibu.kost2Reallocation.detect")}
        </Button>
      </div>
      {error && <FormAlert tone="error">{error}</FormAlert>}
      {result && (
        <>
          <h3 className="text-sm font-semibold">
            {t("fibu.kost2Reallocation.detected")} ({result.detected.length})
          </h3>
          <TimesheetsTable rows={result.detected} />
        </>
      )}
      {!readOnly && (
        <div className="flex flex-wrap items-end gap-2">
          <div className="min-w-72 flex-1">
            <TaskSelect
              value={taskId}
              onChange={setTaskId}
              aria-label={t("fibu.kost2Reallocation.searchTask")}
            />
          </div>
          <Input
            className="w-56"
            placeholder={t("description")}
            aria-label={t("description")}
            value={text}
            onChange={(e) => setText(e.target.value)}
          />
          <Button
            type="button"
            variant="outline"
            disabled={taskId == null || load.isPending}
            onClick={() => load.mutate(taskId)}
          >
            {t("search")}
          </Button>
        </div>
      )}
      {result && result.found.length > 0 && (
        <>
          <h3 className="text-sm font-semibold">
            {t("fibu.kost2Reallocation.found")} ({manualIds.size} /{" "}
            {result.found.length})
          </h3>
          <TimesheetsTable
            rows={result.found}
            selected={manualIds}
            onToggle={readOnly ? undefined : toggle}
          />
        </>
      )}
    </div>
  );
}
