/**
 * Rewrites the calendar backend's legacy team event edit urls onto this app's route shape.
 *
 * The `/rs/calendar/action` endpoint is shared with the timesheet and cal-event editors and with the
 * legacy React calendar, so its output must not be changed server-side (see MIGRATION notes): for a slot
 * select over a calendar it answers `/teamEvent/edit?…&calendar=<id>` for a new event and
 * `/teamEvent/edit/<id>` for an existing one (a resize or drag). This app routes them as `/teamEvent/new`
 * and `/teamEvent/<id>` (see the `[id]` route), so the translation happens here, on the way to the router.
 *
 * The new-event query is kept — the calendar the slot was drawn in and the slot's start and end are what
 * preset the form (`TeamEventPagesRest.newBaseDTO`). The existing-event query is dropped, as it is for a
 * timesheet: the event is loaded by id. A drag or resize therefore opens the event rather than opening it
 * already moved; repositioning without a form is a later phase, together with the recurrence handling the
 * same url carries `origStartDate` for.
 *
 * Pure and total: a url that is not a team event edit url — a timesheet's, an absolute one, anything
 * unexpected — is returned unchanged, so this can wrap every action url without a second thought.
 */

/** The legacy prefix, with and without a trailing id. */
const EDIT_PREFIX = "/teamEvent/edit";

export function toTeamEventRoute(url: string): string {
  if (!url.startsWith(EDIT_PREFIX)) return url;

  // Split the query off first: a new event keeps its preset parameters (calendar/start/end), while an
  // existing one is loaded by id.
  const queryAt = url.indexOf("?");
  const path = queryAt === -1 ? url : url.slice(0, queryAt);
  const query = queryAt === -1 ? "" : url.slice(queryAt);

  // Exactly `/teamEvent/edit` → adding an event, preset from the query.
  if (path === EDIT_PREFIX) return `/teamEvent/new${query}`;

  // `/teamEvent/edit/<id>` → editing that event; the query is dropped.
  const rest = path.slice(EDIT_PREFIX.length);
  if (rest.startsWith("/")) {
    const id = rest.slice(1);
    // Only a plain id segment is a match; anything deeper is left as it was.
    if (id.length > 0 && !id.includes("/")) return `/teamEvent/${id}`;
  }
  return url;
}
