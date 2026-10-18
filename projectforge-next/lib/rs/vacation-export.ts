/**
 * The team vacation calendar (`org.projectforge.rest.VacationExportRest`): the vacations of the chosen employees
 * and groups for a period, and the same as Excel workbook. What the user may pick is restricted by the backend
 * (their own groups unless HR or finance staff); the pickers ask the two lookups of the same controller.
 */

import type {
  VacationExportFavorite,
  VacationExportInitial,
  VacationExportRequest,
  VacationExportSelection,
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
/** The users the quick view of an availability offers (AvailabilityQuickViewDialog), freelancers included. */
export const VACATION_EXPORT_USERS_URL = "vacationExport/users?search=:search";

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

/**
 * The quick view of a user's absences: the next `weeks` weeks from the beginning of this week, a single row (none
 * for a user the logged-in user may not pick). Unlike the calendar, nothing is stored.
 */
export function fetchUserAvailabilityView(
  userId: number,
  weeks: number,
  signal?: AbortSignal
): Promise<VacationExportView> {
  return request<VacationExportView>(
    `/rs/vacationExport/userView?userId=${userId}&weeks=${weeks}`,
    { method: "GET" },
    signal
  );
}

/** The Excel workbook, its sheets starting with the month of `from`. */
export function downloadVacationExportExcel(
  body: VacationExportRequest
): Promise<void> {
  return downloadPost("/rs/vacationExport/exportExcel", body);
}

/** The employees and groups of a favorite (as in [VacationExportRequest]). */
export type VacationExportChoice = Pick<
  VacationExportRequest,
  "employeeIds" | "groupIds"
>;

function postFavorites<T>(
  path: string,
  body?: VacationExportChoice
): Promise<T> {
  return request<T>(`/rs/vacationExport/favorites/${path}`, {
    method: "POST",
    body: body ? JSON.stringify(body) : undefined,
  });
}

/** Saves the choice under `name` (a free one, if taken); answers all favorites. */
export function createVacationExportFavorite(
  name: string,
  choice: VacationExportChoice
) {
  return postFavorites<VacationExportFavorite[]>(
    `create?name=${encodeURIComponent(name)}`,
    choice
  );
}

/** Overwrites the employees and groups of the favorite `id`. */
export function updateVacationExportFavorite(
  id: number,
  choice: VacationExportChoice
) {
  return postFavorites<VacationExportFavorite[]>(`update?id=${id}`, choice);
}

export function renameVacationExportFavorite(id: number, newName: string) {
  return postFavorites<VacationExportFavorite[]>(
    `rename?id=${id}&newName=${encodeURIComponent(newName)}`
  );
}

export function deleteVacationExportFavorite(id: number) {
  return postFavorites<VacationExportFavorite[]>(`delete?id=${id}`);
}

/** The employees and groups of the favorite `id` the user may pick. */
export function selectVacationExportFavorite(id: number) {
  return postFavorites<VacationExportSelection>(`select?id=${id}`);
}
