/**
 * The two exports of the employee-salary list, as Wicket's list page offers them in its content menu: one
 * row per salary, and the cost-assignment (DATEV) sheet. Both act on the filter the list is showing, which
 * is why they live in its toolbar and are handed that filter (see PageDef.listActions).
 */

import { downloadPost } from "./download";
import { downloadListExcel } from "./list-export";
import type { MagicFilter } from "./types";

/** REST category of the employee salary - `EmployeeSalaryEntityRest` is mapped to "employeeSalary". */
const ENTITY = "employeeSalary";

/**
 * The filtered salaries as the Excel file Wicket's plain "Excel export" produces - one row per salary.
 *
 * The generic list export of this category, so it goes through [downloadListExcel]. A 404 means the filter
 * matched nothing; the caller says so rather than reporting an error (see EmployeeSalaryListActions).
 */
export function downloadEmployeeSalaryExcel(
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<void> {
  return downloadListExcel(ENTITY, filter, signal);
}

/**
 * The cost-assignment (DATEV) sheet EmployeeSalaryExportDao builds - one row per Kost2, the gross split
 * over the employee's time-sheet bookings.
 *
 * The backend requires a month be set (400 with the reason as its body) and answers 404 for an empty
 * result - the caller turns both into a toast (see EmployeeSalaryListActions).
 */
export function downloadEmployeeSalaryCostAssignmentsExcel(
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<void> {
  return downloadPost(
    `/rs/${ENTITY}/exportCostAssignmentsAsExcel`,
    filter,
    signal
  );
}
