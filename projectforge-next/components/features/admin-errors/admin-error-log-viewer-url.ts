import type { LogGroupDetail, LogGroupEntry } from "@/lib/rs/admin-errors";

/**
 * The admin log viewer at the problem's level, searching for the uri of a request's problem or else the class of
 * the problem's location (`Foo` of `Foo:42`): its search doesn't know the codes, and a normalized message isn't
 * found. Only the last log events since the server's start are there.
 */
export function logViewerUrl(detail: LogGroupDetail): string | null {
  const search = requestUri(detail.sampleRequest) ?? outerClass(detail.entry);
  if (!search) return null;
  // FATAL is never a threshold (see LOG_THRESHOLDS).
  const threshold =
    detail.entry.level === "FATAL" ? "ERROR" : detail.entry.level;
  return `next/adminLogViewer?search=${encodeURIComponent(search)}&threshold=${threshold}`;
}

/** The problem's occurrences in the log files of its last days, also before the server's start (LogFileSearch). */
export function logFileSearchUrl(detail: LogGroupDetail): string {
  return `next/adminLogViewer?problem=${detail.entry.id}`;
}

/**
 * The uri of a request's problem (`GET /rs/foo?x=1` → `/rs/foo`). Only a request's exception has a request (see
 * `ErrorOccurrenceFactory.fromRequestException`), logged with its uri by `GlobalDefaultExceptionHandler`; its
 * location is the root cause's frame, which the logged line needn't contain.
 */
function requestUri(request: string | null | undefined): string | null {
  const uri = request?.split(" ")[1]?.split("?")[0];
  return uri || null;
}

/**
 * The top-level class of the location: a nested class or companion is located as `Foo.Bar`, but searched in the
 * log viewer by its binary name `org.projectforge.Foo$Bar`, which contains `Foo` only.
 */
function outerClass(entry: LogGroupEntry): string | null {
  const className = entry.location?.split(":")[0]?.split(".")[0];
  return className && className !== "?" ? className : null;
}
