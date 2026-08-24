/**
 * Thin wrappers over the calendar REST controllers (`org.projectforge.rest.calendar`). Every call goes
 * through {@link request}/{@link rawRequest} so CSRF and 2FA recovery apply uniformly (see client.ts).
 *
 * The mutating endpoints answer with a {@link CalendarInitPatch} (a subset of the init keys) that the
 * caller merges over the cached `CalendarInit`; `selectCalendarFilter` and `fetchCalendarInit` return
 * a whole one. `events` and `action` report their errors as a plain-text 400 body, not JSON, so they
 * read the body themselves and raise it as the `RsError` message.
 */

import { rawRequest, request, RsError } from "./client";
import type { ResponseAction } from "./types";
import type {
  CalendarActionParams,
  CalendarData,
  CalendarEventsFilter,
  CalendarInit,
  CalendarInitPatch,
  CalendarRefreshResult,
  CalendarState,
} from "./calendar-types";

const BASE = "/rs/calendar";

/** Builds a `?a=b&…` suffix, dropping null/undefined so a caller can pass optionals straight through. */
function query(
  params: Record<string, string | number | boolean | null | undefined>
): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null) search.set(key, String(value));
  }
  const suffix = search.toString();
  return suffix ? `?${suffix}` : "";
}

/**
 * Sends a request whose failure body is plain text rather than the usual JSON, and raises that text as
 * the error message — used by `events` and `action`, whose 400s carry a human-readable reason.
 */
async function requestOrText<O>(
  path: string,
  init: RequestInit,
  signal?: AbortSignal
): Promise<O> {
  const res = await rawRequest(path, init, signal);
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new RsError(
      res.status,
      text || `${res.status} ${res.statusText}: ${path}`
    );
  }
  return (await res.json()) as O;
}

// --- Read ---

/** The full page state (`GET initial`). */
export function fetchCalendarInit(signal?: AbortSignal): Promise<CalendarInit> {
  return request<CalendarInit>(`${BASE}/initial`, { method: "GET" }, signal);
}

/**
 * The events of the visible range (`POST events`). The 50-day cap and the server-side override of the
 * vacation ids live in the backend; see {@link CalendarEventsFilter}.
 */
export function fetchCalendarEvents(
  filter: CalendarEventsFilter,
  signal?: AbortSignal
): Promise<CalendarData> {
  return requestOrText<CalendarData>(
    `${BASE}/events`,
    { method: "POST", body: JSON.stringify(filter) },
    signal
  );
}

/** Re-reads the external subscriptions and reports whether the client should reload (`GET refresh`). */
export function refreshSubscriptions(
  signal?: AbortSignal
): Promise<CalendarRefreshResult> {
  return request<CalendarRefreshResult>(
    `${BASE}/refresh`,
    { method: "GET" },
    signal
  );
}

/**
 * Resolves a calendar interaction (slot select, create, resize, drag-and-drop) to the edit page the
 * client should open (`GET action`). The answer's `url` is a legacy-style path (`/timesheet/edit?…`)
 * that the caller maps onto a `/next` route.
 */
export function fetchCalendarAction(
  params: CalendarActionParams,
  signal?: AbortSignal
): Promise<ResponseAction> {
  return requestOrText<ResponseAction>(
    `${BASE}/action${query({ ...params })}`,
    { method: "GET" },
    signal
  );
}

/** Persists the last view/date/active-calendars so the page reopens where the user left it (`POST storeState`). */
export async function storeCalendarState(
  state: CalendarState,
  signal?: AbortSignal
): Promise<void> {
  await request<unknown>(
    `${BASE}/storeState`,
    { method: "POST", body: JSON.stringify(state) },
    signal
  );
}

// --- Filter mutations (each answers with a CalendarInitPatch) ---

/** Recolours a calendar; `bgColor` unset clears it (`GET changeStyle`). Answers `{activeCalendars, teamCalendars, styleMap}`. */
export function changeCalendarStyle(
  calendarId: number,
  bgColor: string | undefined,
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/changeStyle${query({ calendarId, bgColor })}`,
    { method: "GET" },
    signal
  );
}

/** Shows or hides a calendar (`GET setVisibility`). Answers `{filter, activeCalendars, isFilterModified}`. */
export function setCalendarVisibility(
  calendarId: number,
  visible: boolean,
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/setVisibility${query({ calendarId, visible })}`,
    { method: "GET" },
    signal
  );
}

/** Sets the calendar a new event defaults to (`GET changeDefaultCalendar`). */
export function changeDefaultCalendar(
  id: number,
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/changeDefaultCalendar${query({ id })}`,
    { method: "GET" },
    signal
  );
}

/** Shows another user's timesheets (or one's own when `userId` is unset) (`GET changeTimesheetUser`). */
export function changeTimesheetUser(
  userId: number | undefined,
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/changeTimesheetUser${query({ userId })}`,
    { method: "GET" },
    signal
  );
}

/** Toggles the working-time break events (`GET changeShowBreaks`). */
export function changeShowBreaks(
  showBreaks: boolean,
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/changeShowBreaks${query({ showBreaks })}`,
    { method: "GET" },
    signal
  );
}

/** Sets the time-grid slot size in minutes; must be one of {@link CALENDAR_GRID_SIZES} (`GET changeGridSize`). */
export function changeGridSize(
  size: number,
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/changeGridSize${query({ size })}`,
    { method: "GET" },
    signal
  );
}

/** Sets the first hour scrolled into view (0–23) (`GET changeFirstHour`). */
export function changeFirstHour(
  hour: number,
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/changeFirstHour${query({ hour })}`,
    { method: "GET" },
    signal
  );
}

/** Sets the groups whose members' vacations are shown; body is a bare id array (`POST changeVacationGroups`). */
export function changeVacationGroups(
  groupIds: number[],
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/changeVacationGroups`,
    { method: "POST", body: JSON.stringify(groupIds) },
    signal
  );
}

/** Sets the users whose vacations are shown; body is a bare id array (`POST changeVacationUsers`). */
export function changeVacationUsers(
  userIds: number[],
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/changeVacationUsers`,
    { method: "POST", body: JSON.stringify(userIds) },
    signal
  );
}

// --- Filter favourites ---

/** Saves the current filter under a new name (`GET createNewFilter`). Answers `{filter, filterFavorites, isFilterModified}`. */
export function createCalendarFilter(
  newFilterName: string,
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/createNewFilter${query({ newFilterName })}`,
    { method: "GET" },
    signal
  );
}

/** Overwrites the saved filter with the current state (`GET updateFilter`). Answers `{isFilterModified: false}`. */
export function updateCalendarFilter(
  id: number,
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/updateFilter${query({ id })}`,
    { method: "GET" },
    signal
  );
}

/** Renames a saved filter (`GET renameFilter`). Answers `{filterFavorites}`. */
export function renameCalendarFilter(
  id: number,
  newName: string,
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/renameFilter${query({ id, newName })}`,
    { method: "GET" },
    signal
  );
}

/** Deletes a saved filter (`GET deleteFilter`). Answers `{filterFavorites}`. */
export function deleteCalendarFilter(
  id: number,
  signal?: AbortSignal
): Promise<CalendarInitPatch> {
  return request<CalendarInitPatch>(
    `${BASE}/deleteFilter${query({ id })}`,
    { method: "GET" },
    signal
  );
}

/** Applies a saved filter and re-initialises the page (`GET selectFilter` → full `CalendarInit`). */
export function selectCalendarFilter(
  id: number,
  signal?: AbortSignal
): Promise<CalendarInit> {
  return request<CalendarInit>(
    `${BASE}/selectFilter${query({ id })}`,
    { method: "GET" },
    signal
  );
}
