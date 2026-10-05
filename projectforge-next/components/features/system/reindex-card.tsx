"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DateInput } from "@/components/shared/date-input";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { toast } from "@/lib/toast";
import { startReindex } from "@/lib/rs/system";
import { JobProgress } from "./job-progress";
import { useSystemJob } from "./use-system-job";
import type { SystemAdminData } from "./types";

/**
 * Rebuilds the full-text search indices. Runs through the background-job infrastructure so its
 * progress can be shown inline (see [useSystemJob]) while the run survives navigating away.
 */
export function ReindexCard({ data }: { data: SystemAdminData }) {
  const t = useTranslations();
  const { start, job, running } = useSystemJob(startReindex);
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
                <span>{t("system.admin.reindex.newestEntries._")}</span>
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
                <span>{t("system.admin.reindex.fromDate._")}</span>
              </HintTooltip>
            </Label>
            <DateInput
              id="reindex-from-date"
              value={fromDate}
              onChange={setFromDate}
              aria-label={t("system.admin.reindex.fromDate._")}
            />
          </div>
          <Button size="sm" disabled={running} onClick={() => void onReindex()}>
            {t("system.admin.button.reindex")}
          </Button>
        </div>
        <p className="text-xs text-muted-foreground">
          {t("system.admin.reindexNewestNEntries.note")}
        </p>
        {job && <JobProgress job={job} />}
      </CardContent>
    </Card>
  );
}
