/**
 * The one call of the vacation entries that is neither a list, a read nor a write of the entity: what the
 * form shows beside its fields for the state being edited (`VacationEntityRest.stats`). It answers a plain
 * stats object, not a `ResponseAction`, hence not `postEntityAction`.
 */

import type { VacationFormStats } from "@/components/features/vacation/types";
import { request } from "./client";
import type { PostData } from "./types";

export function fetchVacationStats(
  data: unknown,
  signal?: AbortSignal
): Promise<VacationFormStats> {
  const postData: PostData = { data } as PostData;
  return request<VacationFormStats>(
    "/rs/vacation/stats",
    { method: "POST", body: JSON.stringify(postData) },
    signal
  );
}
