/**
 * The team vacation calendar (`org.projectforge.rest.VacationExportRest`): the vacations of the chosen users
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

/**
 * The lookups of the two pickers, as [EntityMultiAutocomplete] takes them. The users, freelancers included, also
 * for the quick view of an availability (AvailabilityQuickViewDialog).
 */
export const VACATION_EXPORT_USERS_URL = "vacationExport/users?search=:search";
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

/**
 * The quick view of users' absences: the next `weeks` weeks from the beginning of this week, a row per user in the
 * order given (none for a user the logged-in user may not pick). Unlike the calendar, nothing is stored.
 */
export function fetchUserAvailabilityView(
  userIds: number[],
  weeks: number,
  signal?: AbortSignal
): Promise<VacationExportView> {
  return request<VacationExportView>(
    `/rs/vacationExport/userView?userIds=${userIds.join(",")}&weeks=${weeks}`,
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

/** The users and groups of a favorite (as in [VacationExportRequest]). */
export type VacationExportChoice = Pick<
  VacationExportRequest,
  "userIds" | "groupIds"
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

/** Overwrites the users and groups of the favorite `id`. */
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

/** The users and groups of the favorite `id` the user may pick. */
export function selectVacationExportFavorite(id: number) {
  return postFavorites<VacationExportSelection>(`select?id=${id}`);
}
