import { TIMESHEET_PAGE } from "@/components/features/timesheet/timesheet.page";
import { TEAM_EVENT_PAGE } from "@/components/features/teamEvent/teamEvent.page";
import type { NewEntryParams } from "@/hooks/use-entity-detail";
import type { EntityEditModalDescriptor } from "@/store/entity-edit-modal-store";

/** What a calendar interaction resolves to when it edits a timesheet or a team event in the modal. */
type CalendarEditTarget = Pick<
  EntityEditModalDescriptor,
  "page" | "id" | "newParams"
>;

/**
 * The two entities the calendar edits in place rather than on their own page. Both are reached across a
 * feature boundary on purpose — the same acknowledged exception the wizard makes for the group page:
 * "open a timesheet here" *is* a reference to the timesheet page, and rebuilding its form would be a
 * second one to keep in step with the backend.
 */
const ENTITIES = [
  { route: TIMESHEET_PAGE.route, page: TIMESHEET_PAGE },
  { route: TEAM_EVENT_PAGE.route, page: TEAM_EVENT_PAGE },
] as const;

/**
 * Reads a resolved app url — `/timesheet/42`, `/teamEvent/7`, `/timesheet/new?startDate=…` — into the
 * timesheet or team event edit it stands for, or null for anything else (a vacation, an address, a
 * legacy page) so that keeps navigating.
 *
 * The url is this app's own route shape, after `toTimesheetRoute`/`toTeamEventRoute` have rewritten the
 * backend's legacy `/…/edit` urls (see use-calendar-action). A new entry's preset parameters are
 * filtered to what the page declares it starts from (`edit.newEntryParams`), so an unexpected query key
 * never reaches the backend's `fetchNew`.
 */
export function parseCalendarEditTarget(
  url: string
): CalendarEditTarget | null {
  const queryAt = url.indexOf("?");
  const path = queryAt === -1 ? url : url.slice(0, queryAt);
  const query = queryAt === -1 ? "" : url.slice(queryAt + 1);

  for (const { route, page } of ENTITIES) {
    // A new entry, preset from the slot the calendar drew (a break's span, a team event's calendar).
    if (path === `${route}/new`) {
      return {
        page,
        id: null,
        newParams: pickParams(query, page.edit.newEntryParams ?? []),
      };
    }
    // `/<entity>/<id>` — a plain numeric segment, nothing deeper.
    if (path.startsWith(`${route}/`)) {
      const rest = path.slice(route.length + 1);
      const id = Number(rest);
      if (rest.length > 0 && !rest.includes("/") && Number.isInteger(id)) {
        return { page, id };
      }
    }
  }
  return null;
}

/** The declared preset keys present in the query, dropped when there are none. */
function pickParams(
  query: string,
  keys: readonly string[]
): NewEntryParams | undefined {
  const params = new URLSearchParams(query);
  const result: NewEntryParams = {};
  for (const key of keys) {
    const value = params.get(key);
    if (value != null && value !== "") result[key] = value;
  }
  return Object.keys(result).length > 0 ? result : undefined;
}
