"use client";

import type { EventContentArg } from "@fullcalendar/core";
import { HoverCard, HoverCardTrigger } from "@/components/ui/hover-card";
import { cn } from "@/lib/utils";
import { CalendarEventTooltip } from "./calendar-event-tooltip";
import type { CalendarEventExtendedProps } from "@/lib/rs/calendar-types";

/**
 * FullCalendar's per-event body. Month cells show a coloured dot plus title, time-grid and list rows
 * show the time and title (with the optional description below). When the event carries a tooltip the
 * whole body becomes a hover-card trigger — one card per event, which replaces the legacy manual
 * `createPopper`/`destroy` lifecycle and its collision handling comes for free.
 *
 * The event's colours stay FullCalendar's own inline styles (contrast-computed by the backend), so
 * this only lays out the text.
 */
export function CalendarEventContent({ arg }: { arg: EventContentArg }) {
  const props = arg.event.extendedProps as CalendarEventExtendedProps;
  const isMonth = arg.view.type.startsWith("dayGrid");

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
        </div>
      </div>
    </div>
  );

  if (!props.tooltip) return body;

  return (
    <HoverCard openDelay={200} closeDelay={80}>
      <HoverCardTrigger asChild>
        <div className={cn("h-full w-full", isMonth && "overflow-hidden")}>
          {body}
        </div>
      </HoverCardTrigger>
      <CalendarEventTooltip props={props} />
    </HoverCard>
  );
}
