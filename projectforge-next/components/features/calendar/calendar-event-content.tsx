"use client";

import { useEffect, useRef, useState } from "react";
import type { EventContentArg } from "@fullcalendar/core";
import { AiMagicIcon, InformationCircleIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { useFormatContext } from "@/hooks/use-format";
import { useCoarsePointer } from "@/hooks/use-coarse-pointer";
import { formatTimeRange } from "@/lib/format";
import {
  CalendarEventTooltip,
  CalendarTooltipBody,
} from "./calendar-event-tooltip";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import type { CalendarEventExtendedProps } from "@/lib/rs/calendar-types";

/** Hover-open delay, matching the legacy tooltip's, so a glance across events does not flash cards. */
const OPEN_DELAY = 200;

/**
 * FullCalendar's per-event body. Month cells show a coloured dot plus title, time-grid and list rows
 * show the time and title (with the optional description below). When the event carries a tooltip,
 * pointing at the body opens a card next to the cursor (see CalendarEventTooltip for why it is pinned
 * to the cursor rather than anchored to the event).
 *
 * The event's colours stay FullCalendar's own inline styles (contrast-computed by the backend), so
 * this only lays out the text.
 */
export function CalendarEventContent({ arg }: { arg: EventContentArg }) {
  const t = useTranslations();
  const format = useFormatContext();
  // A touch device has no hover, so the card that hovering opens is unreachable; there the event carries
  // an ⓘ that opens it on tap instead (see below). The hover path is left untouched for a mouse.
  const coarsePointer = useCoarsePointer();
  const props = arg.event.extendedProps as CalendarEventExtendedProps;
  const isMonth = arg.view.type.startsWith("dayGrid");
  // The booked span for the tooltip footer (before the duration), only for a timed event with both
  // ends — an all-day entry has no clock time to show. Same locale/time-notation as the grid times.
  const timeRange =
    !arg.event.allDay && arg.event.start && arg.event.end
      ? formatTimeRange(arg.event.start, arg.event.end, format)
      : null;
  // The viewport point the card is pinned to while open, null when closed. Set from where the pointer
  // entered the event, after the open delay.
  const [anchor, setAnchor] = useState<{ x: number; y: number } | null>(null);
  const openTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const cancelOpen = () => {
    if (openTimer.current) clearTimeout(openTimer.current);
    openTimer.current = null;
  };
  const close = () => {
    cancelOpen();
    setAnchor(null);
  };

  // A scroll dismisses the card: the pointer sits still while the grid moves under it, so a card left
  // open would describe an event no longer under the cursor. Capture, because FullCalendar's inner
  // scroller emits a scroll that never reaches a bubbling listener; and the timer is cleared too, so a
  // scroll during the open delay does not still pop a card afterwards. Cleared on unmount.
  useEffect(() => {
    const onScroll = () => close();
    document.addEventListener("scroll", onScroll, true);
    return () => {
      document.removeEventListener("scroll", onScroll, true);
      cancelOpen();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const body = isMonth ? (
    <div className="fc-event-main-frame flex items-center gap-1 overflow-hidden">
      {!arg.event.allDay && (
        <span
          className="fc-daygrid-event-dot"
          style={{ borderColor: arg.borderColor }}
        />
      )}
      {arg.timeText && <span className="fc-event-time">{arg.timeText}</span>}
      <span className="fc-event-title truncate">{arg.event.title}</span>
    </div>
  ) : (
    <div className="fc-event-main-frame">
      {arg.timeText && <div className="fc-event-time">{arg.timeText}</div>}
      <div className="fc-event-title-container">
        <div className="fc-event-title fc-sticky">
          {arg.event.title}
          {props.description && (
            <div className="whitespace-pre-wrap">{props.description}</div>
          )}
          {/* AI time savings, compact: the icon carries the label (see the popover for the full one),
              only present on time-sheet events with a non-zero saving (see TimesheetEventsProvider). */}
          {props.timeSavedByAI && (
            <div className="flex items-center gap-1 opacity-80">
              <HugeiconsIcon
                icon={AiMagicIcon}
                size={12}
                aria-label={t("timesheet.ai.timeSavedByAI._")}
              />
              <span>{props.timeSavedByAI}</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );

  if (!props.tooltip) return body;

  // Touch: the same card behind an ⓘ in the event's corner, opened on tap as a popover (which dismisses
  // on a tap outside). The button stops the tap from reaching FullCalendar's `eventClick` — otherwise it
  // would navigate to the event's edit page instead of showing the details, and stops the pointer from
  // starting a drag on an `editable` event.
  if (coarsePointer) {
    return (
      <div
        className={cn("relative h-full w-full", isMonth && "overflow-hidden")}
      >
        {body}
        <Popover>
          <PopoverTrigger asChild>
            <button
              type="button"
              aria-label={`${t("info")}: ${arg.event.title}`}
              onPointerDown={(e) => e.stopPropagation()}
              onClick={(e) => e.stopPropagation()}
              className="absolute right-0 top-0 flex items-center justify-center rounded-bl bg-black/10 p-0.5 text-current opacity-80 hover:opacity-100"
            >
              <HugeiconsIcon icon={InformationCircleIcon} size={12} />
            </button>
          </PopoverTrigger>
          <PopoverContent className="w-auto max-w-sm text-sm">
            <CalendarTooltipBody props={props} timeRange={timeRange} />
          </PopoverContent>
        </Popover>
      </div>
    );
  }

  return (
    <div
      className={cn("h-full w-full", isMonth && "overflow-hidden")}
      onPointerEnter={(e) => {
        // Freeze the cursor point now; the card opens there after the delay and stays put.
        const { clientX: x, clientY: y } = e;
        cancelOpen();
        openTimer.current = setTimeout(() => setAnchor({ x, y }), OPEN_DELAY);
      }}
      onPointerLeave={close}
    >
      {body}
      {anchor && (
        <CalendarEventTooltip
          props={props}
          anchor={anchor}
          timeRange={timeRange}
        />
      )}
    </div>
  );
}
