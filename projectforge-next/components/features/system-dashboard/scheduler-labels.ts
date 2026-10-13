import type { useTranslations } from "next-intl";
import type { StatusTone } from "@/components/shared/status-pill";
import { formatNumber, type FormatContext } from "@/lib/format";
import type {
  SchedulerJobEntry,
  SchedulerJobStatus,
  SchedulerRunStatus,
} from "@/lib/rs/admin-scheduler";

type T = ReturnType<typeof useTranslations>;

// Spelled out: the i18n export only finds literal keys.
export const JOB_STATUS_KEYS: Record<SchedulerJobStatus, string> = {
  RUNNING: "system.scheduler.status.running",
  OK: "system.scheduler.status.ok",
  FAILED: "system.scheduler.status.failed",
  OVERDUE: "system.scheduler.status.overdue",
  SKIPPED: "system.scheduler.status.skipped",
  INACTIVE: "system.scheduler.status.inactive",
  DISABLED: "system.scheduler.status.disabled",
  PENDING: "system.scheduler.status.pending",
};

export const JOB_STATUS_TONES: Record<SchedulerJobStatus, StatusTone> = {
  RUNNING: "info",
  OK: "success",
  FAILED: "danger",
  OVERDUE: "danger",
  SKIPPED: "info",
  INACTIVE: "neutral",
  DISABLED: "neutral",
  PENDING: "neutral",
};

export const RUN_STATUS_KEYS: Record<SchedulerRunStatus, string> = {
  SUCCESS: "system.scheduler.runStatus.success",
  ERROR: "system.scheduler.runStatus.error",
  SKIPPED: "system.scheduler.runStatus.skipped",
};

export const RUN_STATUS_TONES: Record<SchedulerRunStatus, StatusTone> = {
  SUCCESS: "success",
  ERROR: "danger",
  SKIPPED: "info",
};

const SECOND = 1000;
const MINUTE = 60 * SECOND;
const HOUR = 60 * MINUTE;

const pad = (value: number) => String(value).padStart(2, "0");

/** A duration as people read it: 850 ms, 12.3 s, 4:05 min, 1:02 h. Empty for none. */
export function formatDurationMillis(
  millis: number | null | undefined,
  ctx: FormatContext
): string {
  if (millis == null || millis < 0) return "";
  if (millis < SECOND) return `${formatNumber(millis, ctx, 0)} ms`;
  if (millis < MINUTE) return `${formatNumber(millis / SECOND, ctx, 1)} s`;
  if (millis < HOUR) {
    const seconds = Math.round(millis / SECOND);
    return `${Math.floor(seconds / 60)}:${pad(seconds % 60)} min`;
  }
  const minutes = Math.round(millis / MINUTE);
  return `${Math.floor(minutes / 60)}:${pad(minutes % 60)} h`;
}

/** When the job runs: its cron expression (with zone) or its delay. */
export function scheduleText(
  job: SchedulerJobEntry,
  t: T,
  ctx: FormatContext
): string {
  switch (job.scheduleType) {
    case "CRON":
      return `${job.cron ?? ""} (${job.zone})`;
    case "FIXED_DELAY":
      return t("system.scheduler.schedule.every", {
        arg0: formatDurationMillis(job.delayMillis, ctx),
      });
    case "DISABLED":
      return t("system.scheduler.schedule.disabled");
    default:
      return t("system.scheduler.schedule.invalid");
  }
}
