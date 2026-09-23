/**
 * The personal statistics page (`org.projectforge.rest.PersonalStatisticsRest`), successor of Wicket's
 * `wa/personalStatistics`. A non-entity, standalone read-only page, so it has its own small client here
 * rather than going through `fetchList` / the entity plumbing.
 */

import { request } from "./client";
import type { PersonalStatistics } from "@/components/features/personal-statistics/types";

/** The logged-in user's timesheet-discipline statistics of the last N days (both chart series + legends). */
export function fetchPersonalStatistics(
  signal?: AbortSignal
): Promise<PersonalStatistics> {
  return request<PersonalStatistics>(
    "/rs/personalStatistics",
    { method: "GET" },
    signal
  );
}
