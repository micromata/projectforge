/**
 * State shapes local to the calendar feature. The wire DTOs live in `lib/rs/calendar-types.ts`; these
 * are the derived, client-owned values the hooks and the panel pass around.
 */

import type { CalendarViewKey } from "@/lib/rs/calendar-types";

/**
 * The identity of one events request. Every field that changes the result is part of it, so it doubles
 * as the React Query key (see `eventsKey`). The id arrays are sorted before they enter the key — the
 * cache must not fragment when the user only re-orders the calendar pills.
 *
 * `vacationGroupIds`/`vacationUserIds` are in the key but NOT in the POST body: the server takes those
 * from the persisted filter (see `CalendarEventsFilter`), yet the events still change when they do, so
 * the key has to notice.
 */
export interface EventsRequest {
  start: string;
  end: string;
  view?: CalendarViewKey;
  activeCalendarIds: number[];
  timesheetUserId?: number | null;
  showBreaks?: boolean | null;
  vacationGroupIds: number[];
  vacationUserIds: number[];
  /** The resolved page theme; the server computes event text colours for it (see CalendarEventsFilter). */
  darkMode: boolean;
  /** Bumped by `?hash` and the manual refresh to force a refetch without any parameter change. */
  nonce: number;
}

/** The visible span and view FullCalendar reports on `datesSet`; drives the events request and `storeState`. */
export interface CalendarRange {
  start: string;
  end: string;
  view: CalendarViewKey;
}

/** One row of a parsed event tooltip: a label and its (possibly multi-line) value. */
export interface TooltipRow {
  label: string;
  value: string;
  multiline: boolean;
}
