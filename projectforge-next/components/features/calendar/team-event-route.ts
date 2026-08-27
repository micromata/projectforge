/**
 * Rewrites the calendar backend's legacy team event edit urls onto this app's route shape.
 *
 * The `/rs/calendar/action` endpoint is shared with the timesheet and cal-event editors and with the
 * legacy React calendar, so its output must not be changed server-side (see MIGRATION notes): for a slot
 * select over a calendar it answers `/teamEvent/edit?…&calendar=<id>` for a new event and
 * `/teamEvent/edit/<id>` for an existing one (a resize or drag). This app routes them as `/teamEvent/new`
 * and `/teamEvent/<id>` (see the `[id]` route), so the translation happens here, on the way to the router.
 *
 * The query is kept in both cases. A new event's `calendar`/`start`/`end` preset the form
 * (`TeamEventPagesRest.newBaseDTO`). An existing event's `startDate`/`endDate` carry the dragged or
 * resized position and `origStartDate`/`origEndDate` the occurrence that was moved, so the event opens
 * already at its new place; `parseCalendarEditTarget` turns them into the dirtying prefill that lets the
 * move be saved (and, for a series, tells the backend which occurrence a single/future edit acts on).
 *
 * Pure and total: a url that is not a team event edit url — a timesheet's, an absolute one, anything
 * unexpected — is returned unchanged, so this can wrap every action url without a second thought.
 */

/** The legacy prefix, with and without a trailing id. */
const EDIT_PREFIX = "/teamEvent/edit";

export function toTeamEventRoute(url: string): string {
  if (!url.startsWith(EDIT_PREFIX)) return url;

  // Split the query off first: a new event carries its preset parameters (calendar/start/end), an
  // existing one the dragged/resized position (start/end/origStart/origEnd) — both are kept.
  const queryAt = url.indexOf("?");
  const path = queryAt === -1 ? url : url.slice(0, queryAt);
  const query = queryAt === -1 ? "" : url.slice(queryAt);

  // Exactly `/teamEvent/edit` → adding an event, preset from the query.
  if (path === EDIT_PREFIX) return `/teamEvent/new${query}`;

  // `/teamEvent/edit/<id>` → editing that event, at the moved position the query carries.
  const rest = path.slice(EDIT_PREFIX.length);
  if (rest.startsWith("/")) {
    const id = rest.slice(1);
    // Only a plain id segment is a match; anything deeper is left as it was.
    if (id.length > 0 && !id.includes("/")) return `/teamEvent/${id}${query}`;
  }
  return url;
}
