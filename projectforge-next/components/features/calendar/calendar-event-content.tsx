"use client";

import { useEffect, useRef, useState } from "react";
import type { EventContentArg } from "@fullcalendar/core";
import { AiMagicIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { CalendarEventTooltip } from "./calendar-event-tooltip";
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
  const props = arg.event.extendedProps as CalendarEventExtendedProps;
  const isMonth = arg.view.type.startsWith("dayGrid");
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
      {anchor && <CalendarEventTooltip props={props} anchor={anchor} />}
    </div>
  );
}
