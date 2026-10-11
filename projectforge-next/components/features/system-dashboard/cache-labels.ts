import type { StatusTone } from "@/components/shared/status-pill";
import type { CacheEntry, CacheState } from "@/lib/rs/admin-caches";

// Spelled out: the i18n export only finds literal keys.
export const CACHE_STATE_KEYS: Record<CacheState, string> = {
  STUCK: "system.caches.state.stuck",
  REFRESHING: "system.caches.state.refreshing",
  FAILED: "system.caches.state.failed",
  NOT_FILLED: "system.caches.state.notFilled",
  EXPIRED: "system.caches.state.expired",
  OK: "system.caches.state.ok",
};

export const CACHE_STATE_TONES: Record<CacheState, StatusTone> = {
  STUCK: "danger",
  REFRESHING: "info",
  FAILED: "danger",
  NOT_FILLED: "neutral",
  EXPIRED: "neutral",
  OK: "success",
};

/** The order of the states in the table: the problems first. */
const STATE_ORDER: CacheState[] = [
  "STUCK",
  "FAILED",
  "REFRESHING",
  "EXPIRED",
  "NOT_FILLED",
  "OK",
];

export const cacheStateRank = (state: CacheState) => STATE_ORDER.indexOf(state);

/** True while a refresh is running: the list is polled more often then. */
export const isRefreshing = (caches: CacheEntry[] | undefined) =>
  !!caches?.some((it) => it.state === "REFRESHING" || it.state === "STUCK");
