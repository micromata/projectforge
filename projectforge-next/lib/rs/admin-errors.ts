/**
 * The error dashboard (`org.projectforge.rest.admin.AdminErrorsRest`, admin group only, 2FA-gated as ADMIN): the
 * problems of the log aggregation (`LogGroupAdminService`) with their trends, and their status. Times are epoch
 * millis.
 *
 * A non-entity page, so it has its own small client rather than going through the entity plumbing.
 */

import { request } from "./client";
import type { LogLevel } from "./log-viewer";

/** `org.projectforge.common.logging.LogCategory`. */
export type LogCategory =
  | "EXTERNAL"
  | "SECURITY"
  | "DATA"
  | "BUG"
  | "CONFIG"
  | "CLIENT"
  | "UNCLASSIFIED";

/** `LogNotify`: whether and when a problem is mailed. */
export type LogNotify = "NONE" | "DIGEST" | "DIGEST_IF_NEW" | "IMMEDIATE";

/** `LogAudience`: who is mailed. */
export type LogAudience = "DEVELOPER" | "ADMIN" | "SECURITY";

/** `LogGroupStatus`. */
export type LogGroupStatus = "NEW" | "ACKNOWLEDGED" | "IGNORED" | "RESOLVED";

/** `LogGroupStatusFilter`: OPEN are the new and acknowledged problems. */
export type LogGroupStatusFilter =
  | "OPEN"
  | "ALL"
  | "NEW"
  | "ACKNOWLEDGED"
  | "IGNORED"
  | "RESOLVED";

export type LogGroupAction =
  | "ACKNOWLEDGE"
  | "IGNORE"
  | "RESOLVE"
  | "REOPEN"
  | "MUTE"
  | "UNMUTE"
  | "SET_NOTIFY";

/** `LogGroupFilter`. [days]: only problems seen within the last days, 0 for all. */
export interface LogGroupFilter {
  status: LogGroupStatusFilter;
  category?: LogCategory | null;
  search?: string | null;
  days?: number | null;
}

/** `LogGroupEntry`. */
export interface LogGroupEntry {
  id: number;
  code: string;
  category: LogCategory;
  level: LogLevel;
  location?: string | null;
  exceptionClass?: string | null;
  /** The sample message, truncated. */
  message?: string | null;
  firstSeen: number;
  lastSeen: number;
  totalCount: number;
  count24h: number;
  status: LogGroupStatus;
  /** Only while still muted. */
  mutedUntil?: number | null;
  /** Ignored or muted: not reported. */
  muted: boolean;
  /** The effective rule: the admin's override, else the event's one. */
  notify: LogNotify;
  overrideNotify?: LogNotify | null;
  /** A resolved problem that occurred again. */
  regression: boolean;
  /** The occurrences of the last `trendDays` days in bins of `trendBinHours` hours, oldest first. */
  trend: number[];
}

/** `LogGroupSummary`: the key figures of all problems, whatever the filter. */
export interface LogGroupSummary {
  occurrences24h: number;
  newProblems24h: number;
  regressions: number;
  externalProblems24h: number;
  open: number;
}

/** `LogGroupList`. [total] counts all matching problems, [entries] holds the first ones only. */
export interface LogGroupList {
  enabled: boolean;
  entries: LogGroupEntry[];
  total: number;
  summary: LogGroupSummary;
  trendDays: number;
  trendBinHours: number;
}

/** `LogGroupDetail`: the problem with the texts of its event and the sample of an occurrence. */
export interface LogGroupDetail {
  entry: LogGroupEntry;
  /** A known event: otherwise explanation and action are missing. */
  registered: boolean;
  explanation?: string | null;
  action?: string | null;
  eventNotify: LogNotify;
  audience: LogAudience;
  threshold: number;
  sampleMessage?: string | null;
  sampleStackTrace?: string | null;
  sampleRequest?: string | null;
  lastNotified?: number | null;
  reopenedAt?: number | null;
  distinctUsers24h: number;
  /** The last 7 days per hour, oldest first, starting at [hourlyStart]. */
  hourly: number[];
  hourlyStart: number;
  /** The last 30 days per 24 hours, oldest first, starting at [dailyStart]. */
  daily: number[];
  dailyStart: number;
}

/** `LogGroupUpdate`. [muteDays] for MUTE, [notify] for SET_NOTIFY (null: the event's rule). */
export interface LogGroupUpdate {
  ids: number[];
  action: LogGroupAction;
  muteDays?: number | null;
  notify?: LogNotify | null;
}

export function fetchAdminErrors(
  filter: LogGroupFilter,
  signal?: AbortSignal
): Promise<LogGroupList> {
  return request<LogGroupList>(
    "/rs/adminErrors/list",
    { method: "POST", body: JSON.stringify(filter) },
    signal
  );
}

export function fetchAdminErrorDetail(
  id: number,
  signal?: AbortSignal
): Promise<LogGroupDetail> {
  return request<LogGroupDetail>(
    `/rs/adminErrors/detail?id=${id}`,
    { method: "GET" },
    signal
  );
}

/** @return The number of changed problems. */
export function updateAdminErrors(update: LogGroupUpdate): Promise<number> {
  return request<number>("/rs/adminErrors/update", {
    method: "POST",
    body: JSON.stringify(update),
  });
}
