/**
 * The team vacation calendar (`org.projectforge.rest.VacationExportRest`): the vacations of the chosen employees
 * and groups for a period, and the same as Excel workbook. What the user may pick is restricted by the backend
 * (their own groups unless HR or finance staff); the pickers ask the two lookups of the same controller.
 */

import type {
  VacationExportInitial,
  VacationExportRequest,
  VacationExportView,
} from "@/components/features/vacation/types";
import { request } from "./client";
import { downloadPost } from "./download";

export const VACATION_EXPORT_QUERY_KEY = ["vacationExport"] as const;

/** The lookups of the two pickers, as [EntityMultiAutocomplete] takes them. */
export const VACATION_EXPORT_EMPLOYEES_URL =
  "vacationExport/employees?search=:search";
export const VACATION_EXPORT_GROUPS_URL =
  "vacationExport/groups?search=:search";

export function fetchVacationExportInitial(
  signal?: AbortSignal
): Promise<VacationExportInitial> {
  return request<VacationExportInitial>(
    "/rs/vacationExport/initial",
    { method: "GET" },
    signal
  );
}

/** The calendar of the period; the backend stores the choice for the next visit. */
export function fetchVacationExportView(
  body: VacationExportRequest,
  signal?: AbortSignal
): Promise<VacationExportView> {
  return request<VacationExportView>(
    "/rs/vacationExport/view",
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
}

/** The Excel workbook, its sheets starting with the month of `from`. */
export function downloadVacationExportExcel(
  body: VacationExportRequest
): Promise<void> {
  return downloadPost("/rs/vacationExport/exportExcel", body);
}
