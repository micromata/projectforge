/**
 * The one call of the employee-salary list that is neither a list, a read nor a write of the entity:
 * the Excel export of the filtered list — what Wicket's "Excel export" content-menu entry produces
 * (EmployeeSalaryEntityRest.exportAsExcel).
 */

import { downloadPost } from "./download";
import type { MagicFilter } from "./types";

/**
 * The filtered salaries as the Excel file EmployeeSalaryExportDao builds (the cost id assignments
 * included, calculated from the time-sheet bookings). The backend requires a month be set and answers
 * 404 for an empty result — the caller turns both into a toast (see EmployeeSalaryListActions).
 */
export function downloadEmployeeSalaryExcel(
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<void> {
  return downloadPost("/rs/employeeSalary/exportAsExcel", filter, signal);
}
