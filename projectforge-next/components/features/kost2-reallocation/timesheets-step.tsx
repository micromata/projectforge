"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { FormAlert } from "@/components/shared/form-alert";
import { Button } from "@/components/ui/button";
import {
  fetchKost2ReallocationTimesheets,
  type Kost2ReallocationTimesheets,
} from "@/lib/rs/kost2-reallocation";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { HintedHeading } from "./hinted-label";
import { TimesheetsTable } from "./timesheets-table";

/**
 * Step 2: the time sheets the run moves to the cost units of the target project. They are detected: their task is in
 * the task tree of the target project, but their cost unit isn't. Moving time sheets in the task tree is not part of
 * the run, it is done beforehand.
 */
export function TimesheetsStep({ runId }: { runId: number }) {
  const t = useTranslations();
  const [result, setResult] = useState<Kost2ReallocationTimesheets | null>(
    null
  );
  const [error, setError] = useState<string | null>(null);

  const load = useMutation({
    mutationFn: async () => {
      const res = await fetchKost2ReallocationTimesheets({ runId });
      if (res.kind === "refused") {
        setError(res.validationErrors.map((e) => e.message).join(" "));
        return;
      }
      setError(null);
      setResult(res.value);
    },
  });

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-2">
        <HintTooltip text={t("fibu.kost2Reallocation.tooltip.detect")}>
          <Button
            type="button"
            variant="outline"
            disabled={load.isPending}
            onClick={() => load.mutate()}
          >
            {t("fibu.kost2Reallocation.detect")}
          </Button>
        </HintTooltip>
      </div>
      {error && <FormAlert tone="error">{error}</FormAlert>}
      {result && (
        <>
          <HintedHeading
            label={`${t("fibu.kost2Reallocation.detected")} (${result.detected.length})`}
            hint={t("fibu.kost2Reallocation.tooltip.detected")}
          />
          <TimesheetsTable rows={result.detected} />
        </>
      )}
    </div>
  );
}
