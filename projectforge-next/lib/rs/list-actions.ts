/**
 * Maintenance actions every list page of an entity offers (`AbstractPagesRest`).
 *
 * These are the endpoints behind the gear menu the legacy frontend built from `UILayout.pageMenu`
 * (see AbstractPagesRest.createListLayout). This app declares the menu itself (see ListGearMenu), so
 * the urls live here rather than coming from the layout.
 *
 * All three are GETs answering with a plain `ResponseAction` - no `PostData` body and no HTTP 406 -
 * so they go through `request()` instead of the raw protocol of ./dynamic.ts. The two re-index calls
 * answer this client with a job id instead of a finished run; ./jobs.ts covers the rest.
 */

import { request } from "./client";
import type { ResponseAction } from "./types";

/**
 * Re-indexes the entries modified since yesterday. The history is not part of it — only a full run
 * rebuilds that (see BaseDao.reindexClasses4NewestEntries).
 *
 * Answers this client with `variables.jobId`, the id of the background job doing the work; the
 * progress is then polled via ./jobs.ts (see useReindex).
 */
export function reindexNewest(
  entity: string,
  signal?: AbortSignal
): Promise<ResponseAction> {
  return request<ResponseAction>(
    `/rs/${entity}/reindexNewest`,
    { method: "GET" },
    signal
  );
}

/**
 * Rebuilds the whole search index of the entity, history included. Takes minutes on a large table
 * and affects the whole system, so the endpoint rejects everyone but admins (the menu hides the
 * entry as well, see UserStatus.adminUser).
 *
 * Answers with a job id like `reindexNewest`.
 */
export function reindexFull(
  entity: string,
  signal?: AbortSignal
): Promise<ResponseAction> {
  return request<ResponseAction>(
    `/rs/${entity}/reindexFull`,
    { method: "GET" },
    signal
  );
}

/**
 * Drops the filter the backend stores for this user *and* the stored grid state, then answers with a
 * RELOAD action and an empty filter. The caller has to clear its own state to match - nothing of the
 * server's answer is applied automatically.
 */
export function resetListFilter(
  entity: string,
  signal?: AbortSignal
): Promise<ResponseAction> {
  return request<ResponseAction>(
    `/rs/${entity}/filter/reset`,
    { method: "GET" },
    signal
  );
}
