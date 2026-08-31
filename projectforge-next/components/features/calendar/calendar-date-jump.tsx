"use client";

import { useState, type RefObject } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverAnchor,
  PopoverContent,
} from "@/components/ui/popover";
import { MonthGrid } from "@/components/shared/month-grid";
import { useDatePickerLocale } from "@/components/shared/use-date-picker-locale";
import { useFormatContext } from "@/hooks/use-format";
import { dateOf } from "@/lib/date-parse";

/**
 * The jump-to-date popover for the calendar header: a month grid whose year dropdown reaches a
 * century back, so a date years ago is one pick away instead of dozens of clicks on the prev button.
 *
 * It has no trigger of its own — the calendar's own toolbar button next to "today" opens it (see
 * FullCalendarPanel, which registers that button and portals the icon into it). Open state and the
 * anchor element are therefore passed in, and the popover positions itself against that button via
 * `anchorRef`.
 *
 * Unlike [DateInput] this holds no value — the date currently shown already sits in the calendar's
 * own title, so nothing is displayed twice and no day stays marked. On opening it lands on the month
 * the calendar is on (read through `getCurrentDate`) for orientation; a pick navigates there via
 * `onGoto` and FullCalendar's `datesSet` drives the events refetch, so this component owns no query.
 */
export function CalendarDateJump({
  open,
  onOpenChange,
  anchorRef,
  onGoto,
  getCurrentDate,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** The toolbar button the popover anchors to; also the node the icon is portaled into. */
  anchorRef: RefObject<HTMLElement | null>;
  onGoto: (date: Date) => void;
  getCurrentDate: () => Date | null;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const pickerLocale = useDatePickerLocale();
  const [month, setMonth] = useState(() => new Date());

  // Land on the month the calendar is currently showing each time it opens, not on wherever it was
  // browsed to last — the popover is a jump-off point, so it should start where the user is. Adjusted
  // on the render that opens it rather than in an effect, which is what React prescribes for state
  // derived from a prop (https://react.dev/learn/you-might-not-need-an-effect) and avoids a cascading
  // render.
  const [wasOpen, setWasOpen] = useState(open);
  if (open !== wasOpen) {
    setWasOpen(open);
    if (open) setMonth(getCurrentDate() ?? new Date());
  }

  function pick(iso: string | null) {
    onOpenChange(false);
    const date = dateOf(iso);
    if (date) onGoto(date);
  }

  // Browsing the month — the year/month dropdowns or the grid's own arrows — moves the calendar right
  // away, so a jump years back needs only the year dropdown, no day click. The popover stays open so
  // the browsing can continue; picking a day is what commits and closes it.
  function browse(next: Date) {
    setMonth(next);
    onGoto(next);
  }

  return (
    <Popover open={open} onOpenChange={onOpenChange}>
      {/* The anchor is the toolbar button, which exists by the time this opens; the cast drops the
          null the ref carries while the calendar is still mounting. */}
      <PopoverAnchor virtualRef={anchorRef as RefObject<HTMLElement>} />
      <PopoverContent
        align="end"
        className="w-auto p-0"
        aria-label={t("calendar.chooseDate")}
        // A click on the toolbar button is what toggles this open/closed; without excluding it, that
        // same click would first register as an interaction outside and close the popover, then the
        // button's own handler would reopen it.
        onInteractOutside={(event) => {
          if (anchorRef.current?.contains(event.target as Node)) {
            event.preventDefault();
          }
        }}
        // The toolbar button keeps its focus; Radix must not pull it into the closed popover.
        onCloseAutoFocus={(event) => event.preventDefault()}
      >
        <MonthGrid
          value={undefined}
          month={month}
          onMonthChange={browse}
          onPick={pick}
          locale={pickerLocale}
          weekStartsOn={ctx.weekStartsOn}
        />
        <div className="flex border-t p-2">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="flex-1 text-xs"
            onClick={() => {
              onOpenChange(false);
              onGoto(new Date());
            }}
          >
            {t("calendar.today")}
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  );
}
