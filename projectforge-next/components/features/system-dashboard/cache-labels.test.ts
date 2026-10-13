import { describe, expect, it } from "vitest";
import type { CacheEntry, CacheState } from "@/lib/rs/admin-caches";
import { cacheStateRank, isRefreshing } from "./cache-labels";

const entry = (state: CacheState): CacheEntry =>
  ({ id: state, state, status: {} }) as unknown as CacheEntry;

describe("cache labels", () => {
  it("ranks the problems first", () => {
    expect(cacheStateRank("STUCK")).toBeLessThan(cacheStateRank("REFRESHING"));
    expect(cacheStateRank("FAILED")).toBeLessThan(cacheStateRank("OK"));
    expect(cacheStateRank("NOT_FILLED")).toBeLessThan(cacheStateRank("OK"));
  });

  it("is refreshing while a cache refreshes or is stuck", () => {
    expect(isRefreshing(undefined)).toBe(false);
    expect(isRefreshing([entry("OK"), entry("FAILED")])).toBe(false);
    expect(isRefreshing([entry("OK"), entry("REFRESHING")])).toBe(true);
    expect(isRefreshing([entry("STUCK")])).toBe(true);
  });
});
