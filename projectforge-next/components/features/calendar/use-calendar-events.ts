"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { fetchCalendarEvents } from "@/lib/rs/calendar";
import type { CalendarEventsFilter } from "@/lib/rs/calendar-types";
import { CALENDAR_EVENTS_KEY } from "./use-calendar-init";
import type { EventsRequest } from "./types";

const sorted = (ids: number[]) => [...ids].sort((a, b) => a - b);

/**
 * The stable query key for an events request. The id arrays are sorted so re-ordering the calendar
 * pills does not fragment the cache; the vacation ids are here (they change the result) even though
 * they never go into the request body — the server reads those from the persisted filter.
 */
export function eventsKey(request: EventsRequest) {
  return [
    ...CALENDAR_EVENTS_KEY,
    {
      start: request.start,
      end: request.end,
      view: request.view ?? null,
      activeCalendarIds: sorted(request.activeCalendarIds),
      timesheetUserId: request.timesheetUserId ?? null,
      showBreaks: request.showBreaks ?? null,
      vacationGroupIds: sorted(request.vacationGroupIds),
      vacationUserIds: sorted(request.vacationUserIds),
      darkMode: request.darkMode,
      nonce: request.nonce,
    },
  ] as const;
}

/**
 * The events of the visible range. `null` while the range is unknown (before the first `datesSet`),
 * which disables the query. Keeps the previous answer on screen while the next loads, so a month change
 * does not flash an empty grid.
 */
export function useCalendarEvents(
  request: EventsRequest | null,
  timeZone?: string
) {
  return useQuery({
    queryKey: request ? eventsKey(request) : [...CALENDAR_EVENTS_KEY, "idle"],
    queryFn: ({ signal }) => {
      const filter: CalendarEventsFilter = {
        start: request!.start,
        end: request!.end,
        view: request!.view,
        timesheetUserId: request!.timesheetUserId,
        showBreaks: request!.showBreaks,
        activeCalendarIds: request!.activeCalendarIds,
        useVisibilityState: true,
        timeZone,
        darkMode: request!.darkMode,
      };
      return fetchCalendarEvents(filter, signal);
    },
    enabled: !!request,
    placeholderData: keepPreviousData,
  });
}
