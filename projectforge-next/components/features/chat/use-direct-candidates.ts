"use client";

import { useMemo } from "react";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { useEntityLookup } from "@/hooks/use-entity-lookup";

/**
 * The users matching a search term, so they can be written to (directly or confidentially) from the search,
 * those with an existing direct message included. The lookup is the user selection (`user/autosearch`), i.e. only users the user may see;
 * opening the conversation is checked again by the server.
 */
export function useDirectCandidates(term: string): EntityRef[] {
  const search = useDebouncedValue(term.trim());
  const { entries } = useEntityLookup<EntityRef>({
    url: "user/autosearch?search=:search",
    search,
    open: true,
    emptyTermSearches: false,
  });
  return useMemo(() => {
    if (search.length < 2 || term.trim() !== search) return [];
    return entries;
  }, [entries, search, term]);
}
