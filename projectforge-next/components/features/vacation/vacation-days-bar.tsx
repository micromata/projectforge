"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate, formatDateRange } from "@/lib/format";
import { cn } from "@/lib/utils";
import { barClassName, dayClassName } from "./vacation-chart-grid";
import {
  dateRanges,
  daysBar,
  type BarDayKind,
} from "./vacation-days-bar-model";
import type { VacationListRow } from "./types";

/** The colours of the form's chart (vacation-chart-grid.ts), so list and form read alike. */
function segmentClassName(kind: BarDayKind): string | undefined {
  if (kind === "uncovered") return barClassName(true, true, true);
  if (kind === "off") return dayClassName({ workingDay: false });
  return barClassName(true, true);
}

/**
 * The list's bar of a vacation: a segment per calendar day at the same scale in every row (cut off after
 * [BAR_MAX_DAYS]), working days in the primary colour, weekends and holidays grey, the days without any
 * substitute on duty red. The tooltip names those days. Hidden on small screens, where the red row remains.
 */
export function VacationDaysBar({ row }: { row: VacationListRow }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const bar = daysBar(row);
  if (!bar) return null;
  const uncovered = dateRanges(row.uncoveredDays ?? []).map(({ from, to }) =>
    from === to ? formatDate(from, ctx) : formatDateRange(from, to, ctx)
  );
  const text = [
    formatDateRange(row.startDate, row.endDate, ctx),
    uncovered.length > 0
      ? `${t("vacation.chart.uncovered")}: ${uncovered.join(", ")}`
      : null,
  ]
    .filter(Boolean)
    .join("\n");

  return (
    <HintTooltip text={text} openOnTap>
      <div
        role="img"
        aria-label={text}
        className="hidden h-2.5 items-stretch gap-px sm:flex"
      >
        {bar.days.map((day) => (
          <span
            key={day.date}
            className={cn(
              "w-1 shrink-0 rounded-[1px]",
              segmentClassName(day.kind),
              day.half && "my-auto h-1/2"
            )}
          />
        ))}
        {bar.truncated && (
          <span className="ml-0.5 self-center text-[10px] leading-none text-muted-foreground">
            …
          </span>
        )}
      </div>
    </HintTooltip>
  );
}
