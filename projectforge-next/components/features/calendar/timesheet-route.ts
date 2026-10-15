/**
 * Rewrites the calendar backend's legacy time sheet edit urls onto this app's route shape.
 *
 * The `/rs/calendar/action` endpoint is shared with the team-event and cal-event editors and with the
 * legacy React calendar, so its output must not be changed server-side (see MIGRATION notes): it still
 * answers `/timesheet/edit` for a new sheet and `/timesheet/edit/<id>` for an existing one. This app
 * routes them as `/timesheet/new` and `/timesheet/<id>` (see the `[id]` route), so the translation
 * happens here, on the way to the router.
 *
 * Pure and total: a url that is not a time sheet edit url — a team event's, an absolute one, anything
 * unexpected — is returned unchanged, so this can wrap every action url without a second thought.
 */

/** The legacy prefix, with and without a trailing id. */
const EDIT_PREFIX = "/timesheet/edit";

export function toTimesheetRoute(url: string): string {
  if (!url.startsWith(EDIT_PREFIX)) return url;

  // Split the query off first: a new sheet carries its preset parameters (start/end/user), an existing
  // one the dragged/resized start and end — both are kept.
  const queryAt = url.indexOf("?");
  const path = queryAt === -1 ? url : url.slice(0, queryAt);
  const query = queryAt === -1 ? "" : url.slice(queryAt);

  // Exactly `/timesheet/edit` → adding a sheet, preset from the query.
  if (path === EDIT_PREFIX) return `/timesheet/new${query}`;

  // `/timesheet/edit/<id>` → editing that sheet, at the moved position the query carries.
  const rest = path.slice(EDIT_PREFIX.length);
  if (rest.startsWith("/")) {
    const id = rest.slice(1);
    // Only a plain id segment is a match; anything deeper is left as it was.
    if (id.length > 0 && !id.includes("/")) return `/timesheet/${id}${query}`;
  }
  return url;
}
