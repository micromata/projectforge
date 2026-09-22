import { TIMESHEET_PAGE } from "@/components/features/timesheet/timesheet.page";
import { TEAM_EVENT_PAGE } from "@/components/features/teamEvent/teamEvent.page";
import type { NewEntryParams } from "@/hooks/use-entity-detail";
import type { EntityEditModalDescriptor } from "@/store/entity-edit-modal-store";

/** What a calendar interaction resolves to when it edits a timesheet or a team event in the modal. */
type CalendarEditTarget = Pick<
  EntityEditModalDescriptor,
  "page" | "id" | "newParams" | "prefill" | "dirtyPrefill"
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
        return { page, id, ...calendarPrefill(query, page) };
      }
    }
  }
  return null;
}

/**
 * What a click or a drag/resize on an existing entry lays over the loaded entry, split by whether it is
 * a change to keep. A **click** on a recurring team event only *views* the occurrence, so its span is a
 * non-dirtying `prefill` over the master the id fetches (the master shows its own start). A **drag or
 * resize** *moves* the entry, so its new position is a `dirtyPrefill` — applied as a real change, which
 * enables Save and persists the move (see EntityEditBody). Everything else — a one-off click, a plain
 * time sheet click — carries no date params and lays nothing over the entry.
 */
function calendarPrefill(
  query: string,
  page: (typeof ENTITIES)[number]["page"]
): Pick<CalendarEditTarget, "prefill" | "dirtyPrefill"> {
  const params = new URLSearchParams(query);
  const startDate = toIsoInstant(params.get("startDate"));
  if (!startDate) return {};
  const endDate = toIsoInstant(params.get("endDate"));

  if (page === TIMESHEET_PAGE) {
    // A time sheet only ever reaches here dragged/resized (a plain click carries no query); only its
    // period moves, mapped to the sheet's own field names (see timesheet-edit-values).
    const dirtyPrefill: Record<string, unknown> = { startTime: startDate };
    if (endDate) dirtyPrefill.stopTime = endDate;
    return { dirtyPrefill };
  }
  if (page !== TEAM_EVENT_PAGE) return {};

  const allDay = params.get("allDay") === "true";
  // Which occurrence a single/future edit acts on: the moved occurrence's *original* place
  // (`origStartDate`) for a drag/resize, or the clicked occurrence itself when none is given (a click).
  const origStartDate = toIsoInstant(params.get("origStartDate")) ?? startDate;
  const origEndDate = toIsoInstant(params.get("origEndDate")) ?? endDate;
  const values = {
    startDate,
    endDate,
    allDay,
    selectedSeriesEvent: {
      startDate: origStartDate,
      endDate: origEndDate,
      allDay,
    },
  };
  // A drag or resize carries the origin occurrence; a click does not. The former is a move to save.
  return params.has("origStartDate")
    ? { dirtyPrefill: values }
    : { prefill: values };
}

/**
 * A calendar date param to an ISO instant, or null when absent/invalid. Two formats reach here: epoch
 * seconds, the form a click url uses (`useCalendarAction.epochSeconds`), and an ISO instant, the
 * `javaScriptString` the `/action` endpoint answers a drag/resize with (`CalendarServicesRest`).
 */
function toIsoInstant(value: string | null): string | null {
  if (!value) return null;
  if (/^-?\d+$/.test(value)) {
    const seconds = Number(value);
    return Number.isFinite(seconds)
      ? new Date(seconds * 1000).toISOString()
      : null;
  }
  const ms = Date.parse(value);
  return Number.isNaN(ms) ? null : new Date(ms).toISOString();
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
