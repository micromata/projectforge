"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate, formatDateRange } from "@/lib/format";
import { cn } from "@/lib/utils";
import { barClassName, dayClassName } from "./absence-chart-grid";
import {
  BAR_SCALE_DAYS,
  dateRanges,
  daysBar,
  type BarDayKind,
  type DaysBarSource,
} from "./absence-days-bar-model";

/** Days of a squeezed bar still drawn with gaps: segments of at least 2px in its 139px. */
const SQUEEZED_GAP_MAX_DAYS = 46;

/** The colours of the form's chart (absence-chart-grid.ts), so list and form read alike. */
function segmentClassName(kind: BarDayKind): string | undefined {
  if (kind === "uncovered") return barClassName(true, true, true);
  if (kind === "off") return dayClassName({ workingDay: false });
  return barClassName(true, true);
}

/**
 * The bar of an absence (a vacation or an availability entry, in their lists and the vacation account): a segment per
 * calendar day, at the same scale in
 * every row up to [BAR_SCALE_DAYS], a longer one squeezed into that width (cut off after [BAR_MAX_DAYS]); working
 * days in the primary colour, weekends and holidays grey, the days without any substitute on duty red. The
 * tooltip names those days. Hidden on small screens, where the red row remains.
 */
export function AbsenceDaysBar({ row }: { row: DaysBarSource }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const bar = daysBar(row);
  if (!bar) return null;
  const squeezed = bar.days.length > BAR_SCALE_DAYS;
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
        className={cn(
          "hidden h-2.5 items-stretch sm:flex",
          // Squeezed into the width of [BAR_SCALE_DAYS] days; without gaps once a segment would get thinner than 2px.
          squeezed && "w-[139px]",
          !squeezed || bar.days.length <= SQUEEZED_GAP_MAX_DAYS
            ? "gap-px"
            : undefined
        )}
      >
        {bar.days.map((day) => (
          <span
            key={day.date}
            className={cn(
              squeezed
                ? "min-w-0 flex-1 rounded-[0.5px]"
                : "w-1 shrink-0 rounded-[1px]",
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

/**
 * The duration of an absence as its lists and the vacation account show it: the working days, formatted by the
 * backend, and beside them the period as a bar ([AbsenceDaysBar]).
 */
export function AbsenceDaysCell({
  formatted,
  row,
}: {
  formatted?: string | null;
  row: DaysBarSource;
}) {
  return (
    <div className="flex items-center gap-3">
      <span className="w-8 shrink-0 text-right tabular-nums">{formatted}</span>
      <AbsenceDaysBar row={row} />
    </div>
  );
}
