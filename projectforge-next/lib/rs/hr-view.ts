/**
 * The HR view ("Personalplanung", `org.projectforge.rest.hr.HRViewRest`), successor of Wicket's `wa/hrList`. A
 * non-entity, standalone page, so it has its own small client rather than going through the entity plumbing.
 */

import { request } from "./client";
import type { HrView, HrViewQuery } from "@/components/features/hr-view/types";

/** The view of the given options; those left out are the ones of the last visit (at first the current week). */
export function fetchHrView(
  query: HrViewQuery,
  signal?: AbortSignal
): Promise<HrView> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value != null) params.set(key, String(value));
  }
  const s = params.toString();
  return request<HrView>(
    `/rs/hrView${s ? `?${s}` : ""}`,
    { method: "GET" },
    signal
  );
}
