/**
 * The calls a time sheet form needs beyond the generic entity ones — the two the legacy form reached
 * through `UICustomized` widgets, and the two autocompletions of its free-text fields
 * (`org.projectforge.rest.TimesheetPagesRest`).
 *
 * Reads and writes of the entity itself are not here: they are the generic `fetchOne`/`fetchNew`
 * (client.ts) and `saveOrUpdateEntity` and friends (entity.ts), parameterised with the category —
 * a time sheet is saved exactly like a book.
 */

import { request } from "./client";
import { downloadPost } from "./download";
import { fetchAutoCompletion } from "./dynamic";
import type { TimesheetDetail } from "@/components/features/timesheet/types";
import type { MagicFilter } from "./types";

const ENTITY = "timesheet";

/**
 * The filtered time sheets as the Excel file of `TimesheetExport` — the legacy list's "Excel export".
 * Acts on the filter the list is showing, so the download is exactly the rows on screen.
 */
export function downloadTimesheetExcel(
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<void> {
  return downloadPost(`/rs/${ENTITY}/exportAsExcel`, filter, signal);
}

/**
 * The subscription URL of the time sheet calendar feed — the legacy list's "ics export". It carries the
 * user's personal, encrypted token, so it is shown for the user to subscribe to rather than downloaded
 * (`TimesheetPagesRest.getIcsExportUrl`, see calendar.icsExport.securityAdvice). The logged-in user by
 * default.
 */
export function fetchTimesheetIcsUrl(
  userId?: number,
  signal?: AbortSignal
): Promise<{ url: string }> {
  const query = userId != null ? `?userId=${userId}` : "";
  return request<{ url: string }>(
    `/rs/${ENTITY}/icsExportUrl${query}`,
    { method: "GET" },
    signal
  );
}

/** What `timesheet/recentList` answers with (`TimesheetPagesRest.RecentTimesheets`). */
export interface RecentTimesheets {
  /**
   * The user's last time sheets, most recent first, each with a `counter` the backend numbered them
   * with — a key for the list, since a recent entry has no id of its own (it is a *template*, built
   * from `TimesheetRecentService`, not a stored sheet).
   *
   * `startTime`/`stopTime` are deliberately absent: what a recent entry offers is the *what* (task,
   * cost unit, location, reference, description), never the *when*.
   */
  timesheets: TimesheetDetail[];
  /**
   * Whether this installation has cost units at all (`SystemInfoCache.isCost2EntriesExists`) — the
   * backend's answer to whether the cost unit column of the recent list is worth its width.
   */
  cost2Visible: boolean;
}

export function fetchRecentTimesheets(
  signal?: AbortSignal
): Promise<RecentTimesheets> {
  return request<RecentTimesheets>(
    `/rs/${ENTITY}/recentList`,
    { method: "GET" },
    signal
  );
}

/** What `selectRecent` answers: the merged sheet in the UPDATE action's `data` variable. */
interface SelectRecentResult {
  variables?: { data?: TimesheetDetail };
}

/**
 * Applies a recent entry (or a template) to the sheet being edited: the backend answers with the
 * merged sheet and the full task behind it, which is more than the entry carries — the task's path,
 * its cost units and its consumption (`TaskServicesRest.createTask`).
 *
 * A write in shape only: it stores nothing, but posting the sheet on screen is how the backend knows
 * what to merge the entry *into*. Unlike the save endpoints, `TimesheetPagesRest.selectRecent` takes
 * the sheet as a bare `@RequestBody` (no `{ data }` envelope) — the same raw shape the favorites
 * endpoints accept — so it goes through `request`, not the enveloping entity-action helper.
 */
export async function selectRecentTimesheet(
  timesheet: TimesheetDetail,
  signal?: AbortSignal
): Promise<TimesheetDetail | null> {
  const result = await request<SelectRecentResult>(
    `/rs/${ENTITY}/selectRecent`,
    { method: "POST", body: JSON.stringify(timesheet) },
    signal
  );
  return result.variables?.data ?? null;
}

/**
 * A named preset the user saved for themselves — a "template" on the form, a *favorite* in the backend
 * (`TimesheetFavorite`, served under `timesheet/favorites`). Its content is a whole time sheet minus its
 * period, the same as a recent entry.
 */
export interface TimesheetFavorite {
  id: number;
  name: string;
}

const FAVORITES = `/rs/${ENTITY}/favorites`;

/** The list every one of the writes below answers with, so the bar never refetches after one. */
interface FavoritesResult {
  timesheetFavorites?: TimesheetFavorite[];
}

/** What `favorites/select` answers: the sheet the template describes, plus its resolved task. */
interface SelectFavoriteResult {
  data?: TimesheetDetail;
}

export function fetchTimesheetFavorites(
  signal?: AbortSignal
): Promise<TimesheetFavorite[]> {
  return request<TimesheetFavorite[]>(
    `${FAVORITES}/list`,
    { method: "GET" },
    signal
  );
}

/**
 * The sheet a template stands for. Only its `data` is read here: the `variables.task` beside it is the
 * full task node, which this app fetches by id where it needs one (`["taskInfo", id]`).
 */
export async function selectTimesheetFavorite(
  id: number,
  timesheet: TimesheetDetail,
  signal?: AbortSignal
): Promise<TimesheetDetail | null> {
  const result = await request<SelectFavoriteResult>(
    `${FAVORITES}/select`,
    { method: "POST", body: JSON.stringify({ id, timesheet }) },
    signal
  );
  return result.data ?? null;
}

/** Saves the sheet on screen as a template under `name`, and answers with the new list. */
export async function createTimesheetFavorite(
  name: string,
  timesheet: TimesheetDetail,
  signal?: AbortSignal
): Promise<TimesheetFavorite[]> {
  const result = await request<FavoritesResult>(
    `${FAVORITES}/create`,
    { method: "POST", body: JSON.stringify({ name, timesheet }) },
    signal
  );
  return result.timesheetFavorites ?? [];
}

export async function deleteTimesheetFavorite(
  id: number,
  signal?: AbortSignal
): Promise<TimesheetFavorite[]> {
  const result = await request<FavoritesResult>(
    `${FAVORITES}/delete?id=${id}`,
    { method: "GET" },
    signal
  );
  return result.timesheetFavorites ?? [];
}

export async function renameTimesheetFavorite(
  id: number,
  newName: string,
  signal?: AbortSignal
): Promise<TimesheetFavorite[]> {
  const result = await request<FavoritesResult>(
    `${FAVORITES}/rename?id=${id}&newName=${encodeURIComponent(newName)}`,
    { method: "GET" },
    signal
  );
  return result.timesheetFavorites ?? [];
}

/**
 * References already used on this task or any of its descendants, by this user or anyone else — what
 * makes a reference a way of grouping sheets rather than a free-text field
 * (`timesheet.reference.info`).
 *
 * Narrowed by the task, so the suggestions change with it: `taskId` is dropped while no task is
 * chosen, and the backend then answers over all of them.
 */
export function fetchReferenceSuggestions(
  search: string,
  taskId: number | null,
  signal?: AbortSignal
): Promise<string[]> {
  return fetchAutoCompletion<string>(
    `${ENTITY}/acReference?search=:search`,
    search,
    { taskId },
    signal
  );
}

/**
 * The locations this user has booked before (`AbstractPagesRest.getAutoCompletionForProperty`, which
 * the entity opts into per property — `isAutocompletionPropertyEnabled("location")`).
 *
 * The generic property autocompletion, not one of the time sheet's own endpoints, hence the
 * `property` parameter.
 */
export function fetchLocationSuggestions(
  search: string,
  signal?: AbortSignal
): Promise<string[]> {
  return fetchAutoCompletion<string>(
    `${ENTITY}/autocomplete?property=location&search=:search`,
    search,
    undefined,
    signal
  );
}
