/**
 * The personal statistics page (`org.projectforge.rest.PersonalStatisticsRest`), successor of Wicket's
 * `wa/personalStatistics`. A non-entity, standalone read-only page, so it has its own small client here
 * rather than going through `fetchList` / the entity plumbing.
 */

import { request } from "./client";
import type { PersonalStatistics } from "@/components/features/personal-statistics/types";

/**
 * The logged-in user's timesheet-discipline statistics of the last N days (both chart series + legends) and
 * the invoicing quota state. `showInvoicingQuota` stores the user's switch; omitted, the last choice is used.
 */
export function fetchPersonalStatistics(
  showInvoicingQuota?: boolean,
  signal?: AbortSignal
): Promise<PersonalStatistics> {
  const params =
    showInvoicingQuota == null
      ? ""
      : `?showInvoicingQuota=${String(showInvoicingQuota)}`;
  return request<PersonalStatistics>(
    `/rs/personalStatistics${params}`,
    { method: "GET" },
    signal
  );
}
