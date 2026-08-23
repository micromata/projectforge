"use client";

import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { LOOKUP_PAGE_SIZE, withMaxResults } from "@/lib/rs/autocomplete-url";
import { fetchAutoCompletion } from "@/lib/rs/dynamic";

/** Reopening a picker within the minute answers from the cache instead of asking again. */
const STALE_MS = 60_000;
/** How close to the end of the list counts as "the user is about to run out of entries". */
const NEAR_BOTTOM_PX = 48;

interface ScrollMetrics {
  scrollTop: number;
  scrollHeight: number;
  clientHeight: number;
}

/**
 * Whether this scroll container is at its end. Pure, so the arithmetic can be pinned in a unit test
 * rather than only in a browser (see use-entity-lookup.test.ts).
 */
export function isNearBottom({
  scrollTop,
  scrollHeight,
  clientHeight,
}: ScrollMetrics): boolean {
  return scrollHeight - scrollTop - clientHeight <= NEAR_BOTTOM_PX;
}

export interface EntityLookupOptions {
  /** The lookup url from the layout, with its literal `:search` placeholder; null for a fixed list. */
  url?: string | null;
  search: string;
  /** Other fields of the form the lookup needs as context (see UISelect.autoCompletion.urlParams). */
  params?: Record<string, unknown>;
  /** Only an open picker asks; a closed one keeps what it had. */
  open: boolean;
  /** Characters before a *typed* term is looked up; the backend defaults it to 2. */
  minChars?: number;
}

export interface EntityLookup<T> {
  entries: T[];
  isFetching: boolean;
  /** A further page is on its way, i.e. there are already entries on screen. */
  isLoadingMore: boolean;
  /** The last answer filled its cap, so asking for more may bring more. */
  hasMore: boolean;
  loadMore: () => void;
}

/**
 * The entries a picker offers: the first page as soon as it opens, a longer one while the user
 * scrolls, and the matches of whatever they type (see EntityAutocomplete and DynamicSelect, which
 * both use this).
 *
 * Opening asks with an empty term, which `{category}/autosearch` answers with the beginning of the
 * whole list — "show me what there is" without typing. Not with *all* of it: a cost unit lookup is
 * 1400 entries and takes the backend well over a second, so the box would sit empty exactly where
 * this is meant to help.
 *
 * Loading more raises `maxResults` and asks again, because the endpoint has no offset
 * (AbstractEntityRest.getAutoCompleteObjects takes `search` and `maxResults`). The previous page
 * stays on screen while the longer one is fetched (`keepPreviousData`), so the list grows instead of
 * blinking, and a category that answers an error is not retried — `order/autosearch` answers 400 to
 * anything, and three attempts per keystroke would be three times nothing.
 */
export function useEntityLookup<T>({
  url,
  search,
  params,
  open,
  minChars = 2,
}: EntityLookupOptions): EntityLookup<T> {
  const term = search.trim();
  const [limit, setLimit] = useState(LOOKUP_PAGE_SIZE);
  const [limitFor, setLimitFor] = useState(term);

  // A new term is a new list, and so is the next time the picker is opened: both start at the first
  // page. Adjusted during render rather than in an effect, so no page is ever fetched for the term
  // before it (React's "adjusting state when props change").
  if (limitFor !== term || (!open && limit !== LOOKUP_PAGE_SIZE)) {
    setLimitFor(term);
    setLimit(LOOKUP_PAGE_SIZE);
  }

  const { data, isFetching } = useQuery({
    queryKey: ["entity-lookup", url, term, params, limit],
    queryFn: ({ signal }) =>
      fetchAutoCompletion<T>(withMaxResults(url!, limit), term, params, signal),
    // The empty term is a legitimate lookup now; a half-typed one still isn't, since it would search
    // for a fragment the user is in the middle of writing.
    enabled:
      open && url != null && (term.length === 0 || term.length >= minChars),
    staleTime: STALE_MS,
    placeholderData: keepPreviousData,
    retry: false,
  });

  const entries = data ?? [];
  // The answer carries no total, so a full page is the only hint that there may be another one.
  const hasMore = entries.length >= limit;

  return {
    entries,
    isFetching,
    isLoadingMore: isFetching && entries.length > 0,
    hasMore,
    loadMore: () => {
      if (isFetching || !hasMore) return;
      setLimit((it) => it + LOOKUP_PAGE_SIZE);
    },
  };
}
