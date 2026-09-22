/**
 * The global full text search (`org.projectforge.rest.SearchRest`): the successor of the Wicket `wa/search`.
 *
 * One query endpoint serves both the live hits under the top-nav magnifier and the dedicated search page; they
 * differ only in the set of areas and the per-area limit. Each hit carries the frontend url of its record
 * (`viewUrl`), ready for `resolveMenuUrl` in lib/menu-url — so no per-entity navigation logic lives here.
 */

import { request } from "./client";

/** A single record found. `viewUrl` is `resolveMenuUrl`-ready (e.g. `next/book/17`, `react/konto/edit/5`). */
export interface SearchHit {
  id: number;
  displayName: string;
  secondaryInfo?: string | null;
  viewUrl: string;
}

/** The hits of one area; `hasMore` signals that the area holds further matches beyond `maxPerArea`. */
export interface SearchAreaResult {
  areaId: string;
  title: string;
  hasMore: boolean;
  hits: SearchHit[];
  /**
   * The area's own native list page (`resolveMenuUrl`-ready, e.g. `next/book` or `react/project`), where
   * "more in …" leads so the whole area is searched there. The term is appended as `?q=` (see [areaMoreUrl]);
   * a next list seeds its search box from it client-side, a React/generic list is seeded server-side.
   */
  listUrl: string;
}

/**
 * The "more/show all in <area>" target: the area's native list, pre-filled with the term. `resolveMenuUrl`-ready,
 * so a next list opens client-side and a React/Wicket one via a full load (see use-navigate-menu-url). Takes any
 * area carrying a `listUrl` — both [SearchArea] and [SearchAreaResult] do.
 */
export function areaMoreUrl(area: { listUrl: string }, term: string): string {
  return `${area.listUrl}?q=${encodeURIComponent(term.trim())}`;
}

export interface SearchResponse {
  term: string;
  areas: SearchAreaResult[];
}

/** A searchable area (a registry entry) the current user may use. */
export interface SearchArea {
  areaId: string;
  title: string;
  /** The area's native list page (`resolveMenuUrl`-ready), so a tile links to it via [areaMoreUrl]. */
  listUrl: string;
}

/**
 * Searches `term` across the given `areas` (all accessible ones when omitted), at most `maxPerArea` hits each.
 * The magnifier passes a small `maxPerArea`; the search page a larger one.
 */
export function searchData(
  term: string,
  areas?: string[],
  maxPerArea?: number,
  signal?: AbortSignal
): Promise<SearchResponse> {
  const params = new URLSearchParams({ q: term });
  if (areas && areas.length > 0) params.set("areas", areas.join(","));
  if (maxPerArea != null) params.set("maxPerArea", String(maxPerArea));
  return request<SearchResponse>(
    `/rs/search/query?${params.toString()}`,
    { method: "GET" },
    signal
  );
}

/** The searchable areas the current user may use, in priority order (addresses first). */
export function fetchSearchAreas(signal?: AbortSignal): Promise<SearchArea[]> {
  return request<SearchArea[]>("/rs/search/areas", { method: "GET" }, signal);
}
