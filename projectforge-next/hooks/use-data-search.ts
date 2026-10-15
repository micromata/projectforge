"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { searchData, type SearchResponse } from "@/lib/rs/search";
import { useDebouncedValue } from "@/hooks/use-debounced-value";

/** Below this many characters a search is noise, not a query; the magnifier stays on the menu until then. */
const MIN_TERM_LENGTH = 2;

/**
 * The live data hits for a term, debounced for the top-nav magnifier.
 *
 * Cross-cutting (in hooks/, not the search feature): the magnifier is app chrome, and the same debounce-and-query
 * shape would serve any other quick lookup. Keeps the previous result while the next loads, so the list under the
 * field doesn't blank out between keystrokes.
 *
 * @param maxPerArea Hits per area — small for the magnifier, so several areas fit without scrolling.
 */
export function useDataSearch(term: string, maxPerArea = 3) {
  const debounced = useDebouncedValue(term.trim());
  const enabled = debounced.length >= MIN_TERM_LENGTH;
  return useQuery<SearchResponse>({
    queryKey: ["dataSearch", debounced, maxPerArea],
    queryFn: ({ signal }) =>
      searchData(debounced, undefined, maxPerArea, signal),
    enabled,
    placeholderData: keepPreviousData,
  });
}
