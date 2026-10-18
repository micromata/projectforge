/**
 * The one call of the availability entries that is neither a list, a read nor a write of the entity: what the form
 * shows of the substitutes' absences for the state being edited (`AvailabilityEntityRest.stats`). It answers a plain
 * stats object, not a `ResponseAction`, hence not `postEntityAction`.
 */

import type { AvailabilityFormStats } from "@/components/features/availability/types";
import { request } from "./client";
import type { PostData } from "./types";

export function fetchAvailabilityStats(
  data: unknown,
  signal?: AbortSignal
): Promise<AvailabilityFormStats> {
  const postData: PostData = { data } as PostData;
  return request<AvailabilityFormStats>(
    "/rs/availability/stats",
    { method: "POST", body: JSON.stringify(postData) },
    signal
  );
}
