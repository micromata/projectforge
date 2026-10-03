/**
 * The log viewer (`org.projectforge.rest.admin.LogViewerRest`): the log events of one of the user's own log
 * subscriptions (DATEV import, Merlin, scripting, mass update …) under `/rs/logViewer`, and the last log events of
 * the whole system for admins under `/rs/adminLogViewer` (`AdminLogViewerRest`, 2FA-gated as ADMIN).
 *
 * A non-entity page, so it has its own small client rather than going through the entity plumbing.
 */

import { request } from "./client";

/** `org.projectforge.common.logging.LogLevel`, highest first. */
export type LogLevel = "FATAL" | "ERROR" | "WARN" | "INFO" | "DEBUG" | "TRACE";

/** `LogViewFilter`: only entries of [threshold] or higher matching [search] are shown. */
export interface LogViewFilter {
  threshold: LogLevel;
  search?: string | null;
  autoRefresh?: boolean | null;
  /** The subscription of the user view; null for the admin log viewer. */
  logSubscriptionId?: number | null;
}

/** `LogViewerEvent`. The timestamp is already formatted (user's time zone in the user view, ISO/UTC for admins). */
export interface LogViewerEvent {
  id: number;
  timestamp: string;
  level: LogLevel;
  message?: string | null;
  stackTrace?: string | null;
  /** `user@ip`. */
  user?: string | null;
  userAgent?: string | null;
}

/** `LogViewerRest.LogViewerData`. */
export interface LogViewerData {
  title: string | null;
  filter: LogViewFilter;
  entries: LogViewerEvent[];
  admin: boolean;
  /** The subscription is gone: it expires after one hour without activity and on a restart of the server. */
  subscriptionMissing: boolean;
}

function basePath(admin: boolean): string {
  return admin ? "/rs/adminLogViewer" : "/rs/logViewer";
}

/** The title, start filter and first entries. [id] is the subscription of the user view, ignored for admins. */
export function fetchLogViewer(
  admin: boolean,
  id: number | null,
  signal?: AbortSignal
): Promise<LogViewerData> {
  const query = !admin && id !== null ? `?id=${id}` : "";
  return request<LogViewerData>(
    `${basePath(admin)}/initial${query}`,
    { method: "GET" },
    signal
  );
}

export function queryLogViewer(
  admin: boolean,
  filter: LogViewFilter,
  signal?: AbortSignal
): Promise<LogViewerEvent[]> {
  return request<LogViewerEvent[]>(
    `${basePath(admin)}/query`,
    { method: "POST", body: JSON.stringify(filter) },
    signal
  );
}

/** Hides the entries received so far of the user's subscription (user view only), answers the remaining ones. */
export function resetLogViewer(
  filter: LogViewFilter
): Promise<LogViewerEvent[]> {
  return request<LogViewerEvent[]>("/rs/logViewer/reset", {
    method: "POST",
    body: JSON.stringify(filter),
  });
}
