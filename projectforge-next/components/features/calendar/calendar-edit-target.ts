import { TIMESHEET_PAGE } from "@/components/features/timesheet/timesheet.page";
import { TEAM_EVENT_PAGE } from "@/components/features/teamEvent/teamEvent.page";
import type { NewEntryParams } from "@/hooks/use-entity-detail";
import type { EntityEditModalDescriptor } from "@/store/entity-edit-modal-store";

/** What a calendar interaction resolves to when it edits a timesheet or a team event in the modal. */
type CalendarEditTarget = Pick<
  EntityEditModalDescriptor,
  "page" | "id" | "newParams" | "prefill"
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
        return { page, id, prefill: occurrencePrefill(query, page) };
      }
    }
  }
  return null;
}

/**
 * The clicked occurrence of a recurring team event, as a non-dirtying prefill over the loaded master:
 * the master is fetched by id and shows its own start (`fetchOne`), so the occurrence's span is layered
 * on here, and carried again as `selectedSeriesEvent` — the answer a single/future edit posts to say
 * which day of the series it acts on (see team-event-edit-schema.ts). Absent for a one-off click (no
 * date params) and for the timesheet, which has no occurrences.
 */
function occurrencePrefill(
  query: string,
  page: (typeof ENTITIES)[number]["page"]
): Record<string, unknown> | undefined {
  if (page !== TEAM_EVENT_PAGE) return undefined;
  const params = new URLSearchParams(query);
  const startDate = epochToIso(params.get("startDate"));
  if (!startDate) return undefined;
  const endDate = epochToIso(params.get("endDate"));
  const allDay = params.get("allDay") === "true";
  return {
    startDate,
    endDate,
    allDay,
    selectedSeriesEvent: { startDate, endDate, allDay },
  };
}

/** Epoch seconds (the form the calendar click url uses) to an ISO instant, or null when absent/invalid. */
function epochToIso(value: string | null): string | null {
  if (!value) return null;
  const seconds = Number(value);
  return Number.isFinite(seconds)
    ? new Date(seconds * 1000).toISOString()
    : null;
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
