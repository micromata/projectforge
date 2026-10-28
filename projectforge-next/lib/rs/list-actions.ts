/**
 * Maintenance actions every list page of an entity offers (`AbstractEntityRest`).
 *
 * This app declares the gear menu itself (see ListGearMenu), so the urls live here.
 *
 * Both are POSTs without a body answering with a plain `ResponseAction` - no `PostData` and no
 * HTTP 406 - so they go through `request()` instead of the raw protocol of ./dynamic.ts. The two re-index calls
 * answer this client with a job id instead of a finished run; ./jobs.ts covers the rest.
 */

import { request } from "./client";
import type { ResponseAction } from "./types";

/**
 * Re-indexes the entries modified since yesterday. The history is not part of it — only a full run
 * rebuilds that (see BaseDao.reindexClasses4NewestEntries).
 *
 * Answers with `variables.jobId`, the id of the background job doing the work; the
 * progress is then polled via ./jobs.ts (see useReindex).
 */
export function reindexNewest(
  entity: string,
  signal?: AbortSignal
): Promise<ResponseAction> {
  return request<ResponseAction>(
    `/rs/${entity}/reindexNewest`,
    { method: "POST" },
    signal
  );
}

/**
 * Rebuilds the whole search index of the entity. Takes minutes on a large table and affects the
 * whole system, so there the endpoint rejects everyone but admins (the menu hides the entry as well,
 * see UserStatus.adminUser). An entity with less than `ListMetaData.fullReindexMaxEntries` entries may
 * be rebuilt by everybody (`ListMetaData.fullReindexOnly`).
 *
 * Answers with a job id like `reindexNewest`.
 */
export function reindexFull(
  entity: string,
  signal?: AbortSignal
): Promise<ResponseAction> {
  return request<ResponseAction>(
    `/rs/${entity}/reindexFull`,
    { method: "POST" },
    signal
  );
}
