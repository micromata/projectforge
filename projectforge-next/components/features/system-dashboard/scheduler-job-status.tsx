"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { StatusPill } from "@/components/shared/status-pill";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import type { SchedulerJobEntry } from "@/lib/rs/admin-scheduler";
import { JOB_STATUS_KEYS, JOB_STATUS_TONES } from "./scheduler-labels";

/** The status of a job; why it is inactive, failed or running is told by its tooltip. */
export function SchedulerJobStatus({ job }: { job: SchedulerJobEntry }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const pill = (
    <StatusPill
      tone={JOB_STATUS_TONES[job.status]}
      label={t(JOB_STATUS_KEYS[job.status])}
    />
  );
  const hint =
    job.status === "INACTIVE"
      ? job.inactiveReason
      : job.status === "FAILED"
        ? job.lastError
        : job.status === "RUNNING" && job.runningSince
          ? t("system.scheduler.runningSince", {
              arg0: formatTimestampMinutes(job.runningSince, ctx),
            })
          : job.status === "OVERDUE" && job.nextRun
            ? t("system.scheduler.overdueSince", {
                arg0: formatTimestampMinutes(job.nextRun, ctx),
              })
            : null;
  if (!hint) return pill;
  return (
    <HintTooltip plain openOnTap text={hint}>
      <span>{pill}</span>
    </HintTooltip>
  );
}
