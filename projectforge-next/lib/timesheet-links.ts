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
 * Query parameters a monthly-employee-report row carries into the time sheet list: the reported user, the
 * exact cost unit (or task), and the month range. The list route reads them back (see
 * app/(authenticated)/timesheet/page.tsx) and seeds a transient, cleared filter — the drill-down Wicket's
 * `MonthlyEmployeeReportPage` did with `userId`/`kost2Id`/`taskId`/`startTime`/`stopTime`/`storeFilter=false`.
 * The Kost2 drill-down uses the cost unit's exact id (`kost2.id`), not a number search — the backend runs
 * the same DB query the report total does (see TimesheetPagesRest.preProcessMagicFilter).
 */
export const TIMESHEET_USER_ID_PARAM = "userId";
export const TIMESHEET_USER_NAME_PARAM = "userName";
export const TIMESHEET_KOST2_ID_PARAM = "kost2Id";
export const TIMESHEET_KOST2_LABEL_PARAM = "kost2Label";
export const TIMESHEET_START_DATE_PARAM = "startDate";
export const TIMESHEET_END_DATE_PARAM = "endDate";

/** One matrix row's drill-down target: either a cost unit or a task, filtered by user and month range. */
export interface MonthlyReportDrillDown {
  userId: number;
  userName?: string;
  kost2Id?: number;
  kost2Label?: string;
  taskId?: number;
  taskName?: string;
  /** Month bounds as `yyyy-MM-dd`. */
  startDate: string;
  endDate: string;
}

/**
 * The time sheet list drilled down from a monthly-employee-report row: the reported user, the exact cost
 * unit or task, and the month range, turned into a transient, cleared filter by the list route.
 */
export function monthlyReportDrillDownHref(
  drillDown: MonthlyReportDrillDown
): string {
  const params = new URLSearchParams({
    [TIMESHEET_USER_ID_PARAM]: String(drillDown.userId),
    [TIMESHEET_START_DATE_PARAM]: drillDown.startDate,
    [TIMESHEET_END_DATE_PARAM]: drillDown.endDate,
  });
  if (drillDown.userName)
    params.set(TIMESHEET_USER_NAME_PARAM, drillDown.userName);
  if (drillDown.kost2Id) {
    params.set(TIMESHEET_KOST2_ID_PARAM, String(drillDown.kost2Id));
    if (drillDown.kost2Label)
      params.set(TIMESHEET_KOST2_LABEL_PARAM, drillDown.kost2Label);
  } else if (drillDown.taskId) {
    params.set(TIMESHEET_TASK_ID_PARAM, String(drillDown.taskId));
    if (drillDown.taskName)
      params.set(TIMESHEET_TASK_NAME_PARAM, drillDown.taskName);
  }
  return `next/timesheet?${params.toString()}`;
}

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
