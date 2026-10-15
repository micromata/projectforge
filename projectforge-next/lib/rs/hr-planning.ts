/**
 * The one call of the HR planning form that is neither a read nor a write of the entity: the entries of
 * the week before, for "copy from predecessor".
 */

import { request } from "./client";
import type { HRPlanningEntryDetail } from "@/components/features/hr-planning/types";

/**
 * The entries of the given user's planning of the week before `week`, as copies without ids and without
 * the deleted ones (`HRPlanningEntityRest.getPredecessor`) — empty if that week isn't planned.
 *
 * @param week Any day of the week (ISO date); the backend normalizes it to its Monday.
 */
export function fetchPredecessorEntries(
  userId: number,
  week: string,
  signal?: AbortSignal
): Promise<HRPlanningEntryDetail[]> {
  const params = new URLSearchParams({ userId: String(userId), week });
  return request<HRPlanningEntryDetail[]>(
    `/rs/hrPlanning/predecessor?${params}`,
    { method: "GET" },
    signal
  );
}
