"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Progress } from "@/components/ui/progress";
import { DateInput } from "@/components/shared/date-input";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { toast } from "@/lib/toast";
import { useSystemReindex } from "./use-system-reindex";
import type { SystemAdminData } from "./types";

/**
 * Rebuilds the full-text search indices. Runs through the background-job infrastructure so its
 * progress can be shown inline (see [useSystemReindex]) while the run survives navigating away.
 */
export function ReindexCard({ data }: { data: SystemAdminData }) {
  const t = useTranslations();
  const { start, job, running } = useSystemReindex();
  const [newestNEntries, setNewestNEntries] = useState(
    String(data.reindexNewestNEntries)
  );
  const [fromDate, setFromDate] = useState<string | null>(null);

  async function onReindex() {
    try {
      const parsed = Number.parseInt(newestNEntries, 10);
      await start({
        newestNEntries: Number.isFinite(parsed) ? parsed : null,
        fromDate,
      });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : String(err));
    }
  }

  const percentage = Math.min(100, Math.max(0, job?.progressPercentage ?? 0));

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("system.admin.button.reindex")}</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex flex-col gap-1">
            <Label htmlFor="reindex-newest">
              <HintTooltip
                text={t("system.admin.reindex.newestEntries.tooltip")}
                openOnTap
              >
                <span>{t("system.admin.reindex.newestEntries")}</span>
              </HintTooltip>
            </Label>
            <Input
              id="reindex-newest"
              type="number"
              min={0}
              className="w-32"
              value={newestNEntries}
              onChange={(event) => setNewestNEntries(event.target.value)}
            />
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor="reindex-from-date">
              <HintTooltip
                text={t("system.admin.reindex.fromDate.tooltip")}
                openOnTap
              >
                <span>{t("system.admin.reindex.fromDate")}</span>
              </HintTooltip>
            </Label>
            <DateInput
              id="reindex-from-date"
              value={fromDate}
              onChange={setFromDate}
              aria-label={t("system.admin.reindex.fromDate")}
            />
          </div>
          <Button size="sm" disabled={running} onClick={() => void onReindex()}>
            {t("system.admin.button.reindex")}
          </Button>
        </div>
        <p className="text-xs text-muted-foreground">
          {t("system.admin.reindexNewestNEntries.note")}
        </p>
        {job && (
          <div className="flex flex-col gap-2">
            <Progress
              value={percentage}
              aria-label={job.progressTitle ?? job.title}
              className="h-1.5"
            />
            <div className="flex items-baseline justify-between gap-3 text-xs text-muted-foreground">
              <span>{job.progressTitle}</span>
              <span className="shrink-0 tabular-nums">{percentage}%</span>
            </div>
            {job.progressDetails && (
              <span className="text-xs text-muted-foreground">
                {job.progressDetails}
              </span>
            )}
            {job.errorMessage && (
              <span className="text-xs text-destructive">
                {job.errorMessage}
              </span>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
