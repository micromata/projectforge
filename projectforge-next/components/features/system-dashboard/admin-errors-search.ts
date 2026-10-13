import type { useTranslations } from "next-intl";
import { LOG_LEVEL_KEYS } from "@/components/shared/log-level";
import type { LogGroupEntry } from "@/lib/rs/admin-errors";
import { CATEGORY_KEYS, STATUS_KEYS } from "./admin-errors-labels";

type T = ReturnType<typeof useTranslations>;

/** The status as shown: a regression stands out as such (see AdminErrorStatus). */
export function statusText(entry: LogGroupEntry, t: T): string {
  return entry.regression
    ? t("system.admin.adminErrors.regression")
    : t(STATUS_KEYS[entry.status]);
}

/**
 * The problems matching every word of `term` (case-insensitive) in one of the texts the table shows - message,
 * code, location, exception class and the translated status, level and category. Client-side: the list holds
 * every problem of the server filter (up to its cap).
 */
export function searchAdminErrors(
  entries: LogGroupEntry[],
  term: string,
  t: T
): LogGroupEntry[] {
  const words = term.trim().toLowerCase().split(/\s+/).filter(Boolean);
  if (words.length === 0) return entries;
  return entries.filter((entry) => {
    const text = [
      entry.message,
      entry.code,
      entry.location,
      entry.exceptionClass,
      statusText(entry, t),
      t(LOG_LEVEL_KEYS[entry.level]),
      t(CATEGORY_KEYS[entry.category]),
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();
    return words.every((word) => text.includes(word));
  });
}
