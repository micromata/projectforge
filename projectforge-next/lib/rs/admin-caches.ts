/**
 * The tab "Caches" of the system dashboard (`org.projectforge.rest.admin.AdminCachesRest`, admin group only): all
 * caches (`CacheAdminService`) with their state, last refresh and errors, and their manual refresh. Times are epoch
 * millis.
 */

import { RsError, rawRequest, request } from "./client";

/** `CacheState`. */
export type CacheState =
  | "STUCK"
  | "REFRESHING"
  | "FAILED"
  | "NOT_FILLED"
  | "EXPIRED"
  | "OK";

/** `CacheStatus`. */
export interface CacheStatus {
  /** The simple class name. */
  name: string;
  qualifiedName: string;
  refreshInProgress: boolean;
  refreshRunningSince?: number | null;
  stuck: boolean;
  expired: boolean;
  everFilled: boolean;
  expireTimeMs: number;
  lastRefreshStart?: number | null;
  lastRefreshEnd?: number | null;
  lastRefreshDurationMs?: number | null;
  lastSuccess?: number | null;
  /** The message of the exception of the last refresh, null if it succeeded. */
  lastRefreshError?: string | null;
  refreshCount: number;
  failureCount: number;
}

/** `CacheEntry`. */
export interface CacheEntry {
  id: string;
  state: CacheState;
  status: CacheStatus;
  /** The last refresh as "5 minutes ago", in the user's locale. */
  lastRefreshTimeAgo?: string | null;
}

export function fetchAdminCaches(signal?: AbortSignal): Promise<CacheEntry[]> {
  return request<CacheEntry[]>(
    "/rs/adminCaches/list",
    { method: "GET" },
    signal
  );
}

/** Starts the refresh of the cache in the background. */
export function refreshAdminCache(id: string): Promise<CacheEntry[]> {
  return request<CacheEntry[]>(
    `/rs/adminCaches/refresh?id=${encodeURIComponent(id)}`,
    { method: "POST" }
  );
}

/**
 * Starts the refresh of all caches in the background.
 * @return false, if a refresh of all caches is still running.
 */
export async function refreshAllAdminCaches(): Promise<boolean> {
  const path = "/rs/adminCaches/refreshAll";
  const res = await rawRequest(path, { method: "POST" });
  if (res.status === 409) return false;
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  return true;
}
