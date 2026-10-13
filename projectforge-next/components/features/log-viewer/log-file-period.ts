import {
  INTERVAL_PRESETS,
  type IntervalPreset,
} from "@/components/data-table/history-interval-presets";
import type { FormatContext } from "@/lib/format";
import { nowIso } from "@/lib/user-zone";

/** The period of the log file search given by its two bounds instead of a preset. */
export const CUSTOM_PERIOD = "custom";

export const DEFAULT_PERIOD = "today";

/** The rolling periods of the history filter, as far back as the log files are searched (31 days). */
export const LOG_FILE_PRESETS: IntervalPreset[] = INTERVAL_PRESETS.filter(
  (preset) => preset.id !== "lastDays60" && preset.id !== "lastDays90"
);

/**
 * The bounds a search is run with, as epoch millis: a preset's from now on (read at the time of the search, so
 * a repeated search includes the newest entries), the entered ones of a custom period. A null bound is open
 * (the backend reads the beginning of today resp. now).
 */
export function logFileBounds(
  period: string | null | undefined,
  from: number | null | undefined,
  until: number | null | undefined,
  ctx: FormatContext,
  now: string = nowIso()
): { from: number | null; until: number | null } {
  if (period === CUSTOM_PERIOD) {
    return { from: from ?? null, until: until ?? null };
  }
  const preset =
    LOG_FILE_PRESETS.find((entry) => entry.id === period) ??
    LOG_FILE_PRESETS.find((entry) => entry.id === DEFAULT_PERIOD)!;
  const start = preset.from(now, ctx);
  return { from: start ? Date.parse(start) : null, until: null };
}
