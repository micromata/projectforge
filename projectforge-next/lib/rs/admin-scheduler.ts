/**
 * The tab "Scheduler" of the system dashboard (`org.projectforge.rest.admin.AdminSchedulerRest`, admin group only,
 * 2FA-gated as ADMIN): the scheduled jobs (`SchedulerJobAdminService`) with their runs, durations and errors. Times
 * are epoch millis, days ISO dates.
 */

import { RsError, rawRequest, request } from "./client";

/** `SchedulerJobStatus`. */
export type SchedulerJobStatus =
  | "RUNNING"
  | "OK"
  | "FAILED"
  | "OVERDUE"
  | "SKIPPED"
  | "INACTIVE"
  | "DISABLED"
  | "PENDING";

/** `SchedulerRunStatus`. */
export type SchedulerRunStatus = "SUCCESS" | "ERROR" | "SKIPPED";

/** `SchedulerTrigger`. */
export type SchedulerTrigger = "SCHEDULED" | "MANUAL";

/** `ResolvedSchedule`: how the job is scheduled. */
export type SchedulerScheduleType =
  | "CRON"
  | "FIXED_DELAY"
  | "DISABLED"
  | "INVALID";

/** `SchedulerJobEntry`. The counts and durations cover the last 7 days (today included). */
export interface SchedulerJobEntry {
  id: string;
  title: string;
  description?: string | null;
  area: string;
  areaTitle: string;
  scheduleType: SchedulerScheduleType;
  /** The resolved cron expression, placeholders replaced. */
  cron?: string | null;
  delayMillis?: number | null;
  initialDelayMillis?: number | null;
  /** The time zone of the cron expression. */
  zone: string;
  status: SchedulerJobStatus;
  inactiveReason?: string | null;
  runningSince?: number | null;
  lastRun?: number | null;
  /** The last run as "5 minutes ago", in the user's locale. */
  lastRunTimeAgo?: string | null;
  /** Set (translated) if the last run was before the start of the system. */
  lastRunBeforeStart?: string | null;
  lastDurationMs?: number | null;
  lastStatus?: SchedulerRunStatus | null;
  lastError?: string | null;
  lastErrorTime?: number | null;
  nextRun?: number | null;
  overdue: boolean;
  runs7d: number;
  errors7d: number;
  skipped7d: number;
  slow7d: number;
  avgDurationMs7d?: number | null;
  maxDurationMs7d?: number | null;
  runNowAllowed: boolean;
}

export interface SchedulerJobList {
  jobs: SchedulerJobEntry[];
  /** Since when the system is up: runs before don't count for overdue jobs. */
  readySince?: number | null;
}

/** `SchedulerPeriodEntry`: the runs of a job on one day or in one month. */
export interface SchedulerPeriodEntry {
  /** ISO date, the first day of the month for a month. */
  periodStart: string;
  runCount: number;
  successCount: number;
  errorCount: number;
  skippedCount: number;
  manualCount: number;
  slowCount: number;
  minDurationMs?: number | null;
  maxDurationMs?: number | null;
  avgDurationMs?: number | null;
  lastErrorMessage?: string | null;
}

/** `SchedulerRunEntry`. */
export interface SchedulerRunEntry {
  start: number;
  durationMs: number;
  status: SchedulerRunStatus;
  trigger: SchedulerTrigger;
  slow: boolean;
  errorMessage?: string | null;
  stackExcerpt?: string | null;
  notes: string[];
}

/** `SchedulerJobDetail`. Days and months oldest first, runs newest first. */
export interface SchedulerJobDetail {
  job: SchedulerJobEntry;
  days: SchedulerPeriodEntry[];
  months: SchedulerPeriodEntry[];
  /** The last runs since the start (in memory). */
  recentRuns: SchedulerRunEntry[];
  /** The failed and slow runs of the database. */
  storedRuns: SchedulerRunEntry[];
  /** After these days the days are merged into months. */
  dailyRetentionDays: number;
}

export function fetchSchedulerJobs(
  signal?: AbortSignal
): Promise<SchedulerJobList> {
  return request<SchedulerJobList>(
    "/rs/adminScheduler/list",
    { method: "GET" },
    signal
  );
}

export function fetchSchedulerJobDetail(
  id: string,
  signal?: AbortSignal
): Promise<SchedulerJobDetail> {
  return request<SchedulerJobDetail>(
    `/rs/adminScheduler/detail?id=${encodeURIComponent(id)}`,
    { method: "GET" },
    signal
  );
}

/** Why "run now" was refused (409): the job is inactive or still running. */
export type SchedulerRunNowConflict = "INACTIVE" | "RUNNING";

/**
 * Starts the job at once.
 * @return null if started, else why not.
 * @throws RsError on other failures (e.g. 404 for an unknown job).
 */
export async function runSchedulerJobNow(
  id: string
): Promise<SchedulerRunNowConflict | null> {
  const path = `/rs/adminScheduler/runNow?id=${encodeURIComponent(id)}`;
  const res = await rawRequest(path, { method: "POST" });
  if (res.status === 409) {
    const body = (await res.text()).replace(/"/g, "").trim();
    return body === "INACTIVE" ? "INACTIVE" : "RUNNING";
  }
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  return null;
}
