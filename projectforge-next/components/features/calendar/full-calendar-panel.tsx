"use client";

import { useCallback, useRef, type RefObject } from "react";
import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import timeGridPlugin from "@fullcalendar/timegrid";
import listPlugin from "@fullcalendar/list";
import interactionPlugin from "@fullcalendar/interaction";
import type {
  CalendarApi,
  DateSelectArg,
  DatesSetArg,
  EventInput,
} from "@fullcalendar/core";
import type { EventDropArg } from "@fullcalendar/core";
import type { EventResizeDoneArg } from "@fullcalendar/interaction";
import { useFormatContext } from "@/hooks/use-format";
import type {
  CalendarViewKey,
  FullCalendarEventDto,
} from "@/lib/rs/calendar-types";
import { cn } from "@/lib/utils";
import { CalendarEventContent } from "./calendar-event-content";
import { useAllDayResizer } from "./use-allday-resizer";
import { useCalendarAction } from "./use-calendar-action";
import { useViewButtons } from "./use-view-buttons";
import { clampVisibleEnd } from "./view-config";
import type { CalendarRange } from "./types";

interface FullCalendarPanelProps {
  events: FullCalendarEventDto[];
  initialView: CalendarViewKey;
  initialDate?: Date;
  gridSize: number;
  firstHour: number;
  /** Shades alternate hours in the time-grid views (the user's `alternateHoursBackground` setting). */
  alternateHoursBackground?: boolean;
  /** Reports the visible span on every navigation; the page turns it into the events request. */
  onRangeChange: (range: CalendarRange) => void;
  /** The page sets its FullCalendar API handle here for `use-goto-date`. */
  apiRef?: RefObject<CalendarApi | null>;
}

const str = (value: unknown): string | undefined =>
  value == null ? undefined : String(value);

/**
 * The FullCalendar itself. Presentational: the events arrive as an array from the query cache (not the
 * `fetchEvents` callback the legacy panel used, which forced the mirror refs and manual `refetchEvents`
 * calls), the view config is memoised rather than frozen into a stale subtree, and interactions are
 * resolved by `use-calendar-action`. The grid size rides a `data-grid-size` attribute the CSS reads.
 */
export function FullCalendarPanel({
  events,
  initialView,
  initialDate,
  gridSize,
  firstHour,
  alternateHoursBackground,
  onRangeChange,
  apiRef,
}: FullCalendarPanelProps) {
  const calendarRef = useRef<FullCalendar>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const { locale, weekStartsOn, hour12 } = useFormatContext();
  const { handleEventClick, requestAction } = useCalendarAction();
  useAllDayResizer(containerRef);

  const { views, headerToolbar, buttonText } = useViewButtons({
    gridSize,
    firstHour,
  });

  const handleDatesSet = useCallback(
    (arg: DatesSetArg) => {
      // From the event, not `calendarRef.current?.getApi()`: FullCalendar fires the first `datesSet`
      // before React has assigned the ref, so reading the ref here yields null on the initial mount
      // and `use-goto-date` would never get an api (see its KDoc). `arg.view.calendar` is always set.
      if (apiRef) apiRef.current = arg.view.calendar;
      const clampedEnd = clampVisibleEnd(arg.start, arg.end);
      onRangeChange({
        start: arg.startStr,
        end: clampedEnd === arg.end ? arg.endStr : clampedEnd.toISOString(),
        view: arg.view.type as CalendarViewKey,
      });
    },
    [apiRef, onRangeChange]
  );

  const handleSelect = useCallback(
    (arg: DateSelectArg) =>
      void requestAction({
        action: "slotSelected",
        startDate: arg.start.toISOString(),
        endDate: arg.end.toISOString(),
        firstHour,
      }),
    [requestAction, firstHour]
  );

  const handleEventChange = useCallback(
    (
      action: "resize" | "dragAndDrop",
      info: EventResizeDoneArg | EventDropArg
    ) => {
      // Always undo: the refetch after the edit is saved shows the authoritative position.
      info.revert();
      const { event, oldEvent } = info;
      const category = event.extendedProps.category as string | undefined;
      const id = event.extendedProps.uid ?? event.extendedProps.dbId;
      if (!category || id == null || event.startEditable !== true) return;
      void requestAction({
        action,
        startDate: event.start?.toISOString(),
        endDate: event.end?.toISOString(),
        category,
        dbId: str(oldEvent.extendedProps.dbId),
        uid: str(oldEvent.extendedProps.uid),
        origStartDate: oldEvent.start?.toISOString(),
        origEndDate: oldEvent.end?.toISOString(),
        firstHour,
      });
    },
    [requestAction, firstHour]
  );

  return (
    <div
      ref={containerRef}
      className={cn(
        "pf-calendar h-full",
        alternateHoursBackground && "pf-calendar-alt"
      )}
      data-grid-size={gridSize}
    >
      <FullCalendar
        ref={calendarRef}
        plugins={[dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin]}
        initialView={initialView}
        initialDate={initialDate}
        headerToolbar={headerToolbar}
        buttonText={buttonText}
        views={views}
        events={events as unknown as EventInput[]}
        eventContent={(arg) => <CalendarEventContent arg={arg} />}
        eventTimeFormat={{ hour: "2-digit", minute: "2-digit", hour12 }}
        locale={locale}
        firstDay={weekStartsOn}
        height="100%"
        // Pin the weekday column headers and the all-day row while the hours scroll, so the column a
        // scrolled-to event belongs to stays labelled.
        stickyHeaderDates
        nowIndicator
        editable
        selectable
        datesSet={handleDatesSet}
        eventClick={handleEventClick}
        select={handleSelect}
        eventResize={(info) => handleEventChange("resize", info)}
        eventDrop={(info) => handleEventChange("dragAndDrop", info)}
      />
    </div>
  );
}
