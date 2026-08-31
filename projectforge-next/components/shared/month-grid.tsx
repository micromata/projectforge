"use client";

import { memo, useMemo } from "react";
import { Calendar } from "@/components/ui/calendar";
import type { FormatContext } from "@/lib/format";
import { dateOf, isoOf } from "@/lib/date-parse";
import type { useDatePickerLocale } from "./use-date-picker-locale";

/** How far the year dropdown reaches back — far enough for a date of birth. */
const YEARS_BACK = 100;
/** ...and ahead, for periods of performance and other planning far into the future. */
const YEARS_AHEAD = 20;

/**
 * The month grid, memoized — and the reason it is a component of its own.
 *
 * The primitive builds its `components` map inline on every render (`components/ui/calendar.tsx`,
 * which must not be edited), so React sees new component *types* each time and remounts the whole
 * grid rather than updating it. Any unrelated re-render of the caller therefore replaced every day
 * button — and when that happened between the mousedown and the mouseup of a click, the browser saw
 * the two halves on different nodes and fired no click at all: the first pick in the calendar was
 * swallowed. Memoizing keeps the grid mounted unless the value, the month or the locale really change.
 *
 * `selected` is derived here rather than passed in, because a fresh `Date` object per render would
 * defeat the memo the same way. The ISO string is the prop; the Date stays inside.
 *
 * Shared by [DateInputCalendar] (the field's popover) and the calendar's jump-to-date control.
 */
export const MonthGrid = memo(function MonthGrid({
  value,
  month,
  onMonthChange,
  onPick,
  locale,
  weekStartsOn,
}: {
  value: string | null | undefined;
  month: Date;
  onMonthChange: (month: Date) => void;
  onPick: (value: string | null) => void;
  locale: ReturnType<typeof useDatePickerLocale>;
  weekStartsOn: FormatContext["weekStartsOn"];
}) {
  const selected = useMemo(() => dateOf(value) ?? undefined, [value]);
  // Without an explicit range the year dropdown stops at the end of the current year (react-day-picker
  // defaults `endMonth` to `endOfYear(today)` as soon as a dropdown caption is used), so a period of
  // performance reaching into the next year could be typed but not picked. The range always covers the
  // date in the field as well, so no value is unreachable by browsing.
  const [startMonth, endMonth] = useMemo(() => {
    const thisYear = new Date().getFullYear();
    const valueYear = selected?.getFullYear() ?? thisYear;
    return [
      new Date(Math.min(thisYear - YEARS_BACK, valueYear), 0, 1),
      new Date(Math.max(thisYear + YEARS_AHEAD, valueYear), 11, 31),
    ];
  }, [selected]);
  return (
    <Calendar
      mode="single"
      selected={selected}
      month={month}
      onMonthChange={onMonthChange}
      // Clicking the already-selected day keeps it (single mode reports that click as a deselect,
      // date === undefined): confirming the date must not empty the field. Clearing is the reset
      // button below and the ✕ in the field, not an accidental second click on the same day.
      onSelect={(date) => onPick(date ? isoOf(date) : (value ?? null))}
      locale={locale}
      weekStartsOn={weekStartsOn}
      captionLayout="dropdown"
      // Always six week rows, even when a month fits in four or five. The row count would
      // otherwise change the popover's height from month to month, Radix would reposition it,
      // and the paging arrows would jump away from under the cursor mid-step.
      fixedWeeks
      startMonth={startMonth}
      endMonth={endMonth}
      // Today only gets a grey background from the primitive, which is hard to tell from a hovered
      // day. A ring in the accent colour reads as "here you are" even when another day is selected;
      // on the selected day itself it sits inside its filled button.
      classNames={{
        today:
          "rounded-(--cell-radius) font-semibold text-primary ring-1 ring-inset ring-primary/70",
      }}
    />
  );
});
