/**
 * The vacation account ("Urlaubskonto", `org.projectforge.rest.VacationAccountRest`), successor of the React page
 * `react/vacationAccount/dynamic`. A non-entity, standalone page, so it has its own small client.
 */

import { request } from "./client";
import type { VacationAccount } from "@/components/features/vacation-account/types";

/**
 * The account of the given employee (HR staff only, ignored for anybody else); without one, the one shown last
 * (HR staff) or the own one.
 */
export function fetchVacationAccount(
  employeeId: number | undefined,
  signal?: AbortSignal
): Promise<VacationAccount> {
  const query = employeeId ? `?employeeId=${employeeId}` : "";
  return request<VacationAccount>(
    `/rs/vacationAccount${query}`,
    { method: "GET" },
    signal
  );
}

/** Drops the stored carry of the current year, so it is computed anew (HR staff only); answers the fresh account. */
export function recalculateRemainingLeave(
  employeeId: number
): Promise<VacationAccount> {
  return request<VacationAccount>(
    `/rs/vacationAccount/recalculate?employeeId=${employeeId}`,
    { method: "POST" }
  );
}
