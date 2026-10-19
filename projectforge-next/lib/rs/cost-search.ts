/**
 * Thin wrapper over the cost search controller (`org.projectforge.rest.fibu.kost.CostSearchRest`): a cost 1
 * or cost 2 unit, picked via the shared `cost1/autosearch` / `cost2/autosearch`, resolved to what finance
 * users copy into other systems.
 */

import { request } from "./client";

export type CostKind = "cost1" | "cost2";

export interface CostDetails {
  /** The raw number (cost 2: `512345678`). */
  number: number;
  /** The long display name, number included (`5.123.45.67 - …`). */
  name: string;
}

export const COST_SEARCH_QUERY_KEY = ["costSearch"] as const;

export function fetchCostDetails(
  kind: CostKind,
  id: number,
  signal?: AbortSignal
): Promise<CostDetails> {
  return request<CostDetails>(
    `/rs/costSearch/${kind}/${id}`,
    { method: "GET" },
    signal
  );
}
