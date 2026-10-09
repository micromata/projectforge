"use client";

import { useTranslations } from "next-intl";
import { Spinner } from "@/components/shared/spinner";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate, formatNumber, formatWeekdayShort } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { DayPlan, DayPlanStatus } from "./types";

/** Literal keys, so the i18n generator finds them (it exports only keys spelled out in the sources). */
const STATUS_KEYS: Record<DayPlanStatus, string> = {
  BOOK: "timesheet.bookDays.status.book",
  PARTIAL: "timesheet.bookDays.status.partial",
  WEEKEND: "timesheet.bookDays.status.weekend",
  HOLIDAY: "timesheet.bookDays.status.holiday",
  OVERLAP: "timesheet.bookDays.status.overlap",
  OVERLAP_UNKNOWN: "timesheet.bookDays.status.overlapUnknown",
};

/** Statuses whose text takes the note (the time of the overlapped time sheet) as argument. */
const WITH_TIME: ReadonlySet<DayPlanStatus> = new Set([
  "OVERLAP",
  "OVERLAP_UNKNOWN",
]);

/**
 * Day by day, what the booking would do: booked (in full or in part) or skipped, and why — the
 * backend's dry run of the very request the primary button sends. An overlap with an existing time
 * sheet skips the day, unless a shared cost element allows it (then it is booked and noted).
 *
 * @param error Why the server refuses the request as it stands (no access, a protected period, …).
 */
export function BookDaysPreview({
  days,
  loading,
  error,
}: {
  days: DayPlan[];
  loading: boolean;
  error: string | null;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const booked = days.filter((day) => day.hours > 0);
  const hours = booked.reduce((sum, day) => sum + day.hours, 0);

  return (
    <div className="flex min-h-0 flex-col gap-1.5">
      <div className="flex items-baseline justify-between gap-2 text-sm">
        <span className="flex items-center gap-2 font-medium">
          {t("timesheet.bookDays.preview")}
          {loading && <Spinner className="h-3 w-3 border-2" />}
        </span>
        {days.length > 0 && (
          <span className="text-muted-foreground">
            {t("timesheet.bookDays.summary", {
              arg0: booked.length,
              arg1: formatNumber(hours, ctx),
            })}
          </span>
        )}
      </div>
      {error && (
        <p className="rounded-md border border-destructive/50 p-3 text-sm text-destructive">
          {error}
        </p>
      )}
      {days.length === 0 ? (
        !error && (
          <p className="rounded-md border border-dashed p-3 text-sm text-muted-foreground">
            {t("timesheet.bookDays.previewEmpty")}
          </p>
        )
      ) : (
        <ul className="max-h-[28rem] overflow-y-auto rounded-md border text-sm">
          {days.map((day) => {
            const skipped = day.hours <= 0;
            const withTime = WITH_TIME.has(day.status);
            return (
              <li
                key={day.date}
                className={cn(
                  "flex items-center gap-3 border-b px-3 py-1 last:border-b-0",
                  skipped && "text-muted-foreground",
                  day.status === "OVERLAP" && "text-destructive",
                  day.status === "OVERLAP_UNKNOWN" &&
                    "text-amber-700 dark:text-amber-500"
                )}
              >
                <span className="w-8 shrink-0">
                  {formatWeekdayShort(day.date, ctx)}
                </span>
                <span className="w-24 shrink-0 tabular-nums">
                  {formatDate(day.date, ctx)}
                </span>
                <span className={cn("min-w-0 flex-1", skipped && "italic")}>
                  {withTime
                    ? t(STATUS_KEYS[day.status], { arg0: day.note ?? "" })
                    : t(STATUS_KEYS[day.status])}
                  {!withTime && day.note && ` – ${day.note}`}
                  {day.sharedOverlap && (
                    <span className="ml-1 text-xs text-muted-foreground">
                      {t("timesheet.bookDays.status.sharedOverlap", {
                        arg0: day.sharedOverlap,
                      })}
                    </span>
                  )}
                </span>
                <span className="w-12 shrink-0 text-right tabular-nums">
                  {day.hours > 0 && `${formatNumber(day.hours, ctx)} h`}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
