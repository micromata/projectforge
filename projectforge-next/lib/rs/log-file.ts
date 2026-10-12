/**
 * The search of the admin log viewer over the log files (`AdminLogViewerRest`, `/rs/adminLogViewer/logFile/*`,
 * 2FA-gated as ADMIN): by text, period, level and logger, with the context of a hit read on demand.
 */

import { request } from "./client";
import type { LogFileEvent, LogFileSearchData } from "./admin-errors";
import type { LogLevel } from "./log-viewer";

/** `LogFileQueryFilter`, also the admin's last search (user pref). */
export interface LogFileQueryFilter {
  /** All words must be found, `-word` must not, `"…"` quotes a phrase. */
  search?: string | null;
  threshold: LogLevel;
  /** Epoch millis; null for the beginning of today resp. now. */
  from?: number | null;
  until?: number | null;
  /** The period chosen (see LOG_FILE_PERIODS), only kept for the next visit. */
  period?: string | null;
  /** Only these loggers (`org.projectforge.business.fibu`), matched against their abbreviations, too. */
  includeLoggers: string[];
  /** Not these loggers: the noise. */
  excludeLoggers: string[];
  /** The number of entries before and after a hit shown as its context. */
  before: number;
  after: number;
}

/** `LogFileContextScope`: which neighbours of a hit are its context. */
export type LogFileContextScope = "ALL" | "USER" | "LOGGER";

/** `LogFileContextRequest`. */
export interface LogFileContextRequest {
  file: string;
  line: number;
  timestampMillis: number;
  user: string;
  logger: string;
  before: number;
  after: number;
  scope: LogFileContextScope;
  excludeLoggers: string[];
}

/** `LogFileContextData`: a hit with its context, oldest first. */
export interface LogFileContextData {
  /** False, if the hit isn't at its position anymore (the current file was rolled since the search). */
  found: boolean;
  before: LogFileEvent[];
  hit?: LogFileEvent | null;
  after: LogFileEvent[];
  /** There are more entries of the scope before resp. after, in the same file. */
  moreBefore: boolean;
  moreAfter: boolean;
  /** The time or size limit was reached before all context was read. */
  truncated: boolean;
}

export function fetchLogFileInitial(
  signal?: AbortSignal
): Promise<LogFileQueryFilter> {
  return request<LogFileQueryFilter>(
    "/rs/adminLogViewer/logFile/initial",
    { method: "GET" },
    signal
  );
}

export function queryLogFile(
  filter: LogFileQueryFilter,
  signal?: AbortSignal
): Promise<LogFileSearchData> {
  return request<LogFileSearchData>(
    "/rs/adminLogViewer/logFile/query",
    { method: "POST", body: JSON.stringify(filter) },
    signal
  );
}

export function fetchLogFileContext(
  context: LogFileContextRequest,
  signal?: AbortSignal
): Promise<LogFileContextData> {
  return request<LogFileContextData>(
    "/rs/adminLogViewer/logFile/context",
    { method: "POST", body: JSON.stringify(context) },
    signal
  );
}
