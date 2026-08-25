/**
 * The one call a team event form needs beyond the generic entity ones: the writable calendars its
 * calendar select offers (`TeamEventPagesRest.getCalendars`).
 *
 * Reads and writes of the event itself are not here: they are the generic `fetchOne`/`fetchNew`
 * (client.ts) and `saveOrUpdateEntity` and friends (entity.ts), parameterised with the category — a
 * team event is saved exactly like a book.
 */

import { request } from "./client";

const ENTITY = "teamEvent";

/** A team calendar as the select offers it (`TeamEventPagesRest.CalendarSelectValue`). */
export interface TeamCalendarOption {
  id: number;
  title: string;
}

/**
 * The calendars this user may write an event into — full-access calendars minus the external
 * subscriptions, which are read-only (the same list the legacy UILayout form embedded). The event's
 * own calendar is not added here, since a new hand-built event always starts from one of these.
 */
export function fetchTeamCalendars(
  signal?: AbortSignal
): Promise<TeamCalendarOption[]> {
  return request<TeamCalendarOption[]>(
    `/rs/${ENTITY}/calendars`,
    { method: "GET" },
    signal
  );
}
