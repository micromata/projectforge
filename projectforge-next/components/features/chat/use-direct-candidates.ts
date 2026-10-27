"use client";

import { useMemo } from "react";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { useEntityLookup } from "@/hooks/use-entity-lookup";
import type { ChatDirectEntry } from "@/lib/rs/chat-types";

/**
 * The users matching a search term the logged-in user has no direct message with yet, so they can be written
 * to from the search. The lookup is the user selection (`user/autosearch`), i.e. only users the user may see;
 * opening the conversation is checked again by the server.
 */
export function useDirectCandidates(
  term: string,
  existing: ChatDirectEntry[] | undefined
): EntityRef[] {
  const search = useDebouncedValue(term.trim());
  const { entries } = useEntityLookup<EntityRef>({
    url: "user/autosearch?search=:search",
    search,
    open: true,
    emptyTermSearches: false,
  });
  return useMemo(() => {
    if (search.length < 2 || term.trim() !== search) return [];
    const known = new Set(existing?.map((entry) => entry.partner.id));
    return entries.filter((user) => !known.has(user.id));
  }, [entries, existing, search, term]);
}
