import type { StatusTone } from "@/components/shared/status-pill";
import type { LogLevel } from "@/lib/rs/log-viewer";

/** The thresholds to choose from, highest first. FATAL is never logged (see `LogLevel.getLevel`). */
export const LOG_THRESHOLDS: readonly LogLevel[] = [
  "ERROR",
  "WARN",
  "INFO",
  "DEBUG",
  "TRACE",
];

/** The i18n key of a level's name. FATAL has no text of its own and is shown as an error. */
export const LOG_LEVEL_KEYS: Record<LogLevel, string> = {
  FATAL: "log.level.error",
  ERROR: "log.level.error",
  WARN: "log.level.warn",
  INFO: "log.level.info",
  DEBUG: "log.level.debug",
  TRACE: "log.level.trace",
};

/** The tone of a level's pill: errors stand out, warnings are tinted, everything else stays quiet. */
export function logLevelTone(level: LogLevel): StatusTone {
  switch (level) {
    case "FATAL":
    case "ERROR":
      return "danger";
    case "WARN":
      return "info";
    default:
      return "neutral";
  }
}

/** The row class of an entry: only errors tint the whole row, so they can be found while scrolling. */
export function logLevelRowClass(level: LogLevel): string | undefined {
  return logLevelTone(level) === "danger" ? "row-red" : undefined;
}
