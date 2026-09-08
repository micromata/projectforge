/**
 * Links into the (migrated) timesheet pages, spelled once.
 *
 * Here rather than beside the timesheet page declaration, because the callers span tiers — the task-tree
 * consumption bar (a data-table cell) and the task form's cross-links (a feature) both jump here, and
 * neither may import the timesheet feature (see the tier rules in projectforge-next/CLAUDE.md). Pure url
 * building, so it belongs in lib.
 *
 * The urls are `next/` menu urls resolved to a client-side route by [resolveMenuUrl]; the timesheet is
 * migrated, so the jump is an in-app navigation, not a full page load into the legacy app.
 */

/**
 * Query parameters a link carries into the time sheet list: the task to filter by, and its name for the
 * filter pill (a caller usually knows the id but not the display name the pill wants). The timesheet list
 * route reads them back (see app/(authenticated)/timesheet/page.tsx) and seeds a transient, cleared
 * filter — the three things Wicket's `ConsumptionBarPanel` did with `taskId`/`clear`/`storeFilter`.
 */
export const TIMESHEET_TASK_ID_PARAM = "taskId";
export const TIMESHEET_TASK_NAME_PARAM = "taskName";

/**
 * The time sheet list of one task. Carries the task id and, when known, its name — the list route turns
 * them into a transient, cleared filter (see the two param names above).
 */
export function timesheetListHref(taskId: number, taskName?: string): string {
  const params = new URLSearchParams({
    [TIMESHEET_TASK_ID_PARAM]: String(taskId),
  });
  if (taskName) params.set(TIMESHEET_TASK_NAME_PARAM, taskName);
  return `next/timesheet?${params.toString()}`;
}

/**
 * The add-a-time-sheet form, preset to one task. The id rides the add url and is handed to the new-entry
 * preset (`taskId` in the timesheet page's `newEntryParams`; `TimesheetPagesRest.newBaseDTO` resolves it
 * into the sheet's task, letting the form auto-pick the single cost unit).
 */
export function timesheetAddHref(taskId: number): string {
  const params = new URLSearchParams({
    [TIMESHEET_TASK_ID_PARAM]: String(taskId),
  });
  return `next/timesheet/new?${params.toString()}`;
}
