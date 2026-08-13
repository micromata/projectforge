"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchListMeta } from "@/lib/rs/client";
import type { ListMetaData } from "@/lib/rs/types";

/**
 * What a hand built list page needs from the backend beside its rows: the filter fields of the entity,
 * the filter the user left the page with, and their saved filters.
 *
 * The filter fields can't be declared in the frontend — the backend derives them per entity from the
 * DAO's search fields — and the two filter states are the user's, stored server-side, so they are the
 * same the legacy list page would restore.
 *
 * The saved-filter list is patched into this cache entry when a favorite is created, renamed or
 * deleted (see useFilterFavorites), so there is one source of truth for it.
 */
export function useListMeta(entity: string) {
  return useQuery<ListMetaData>({
    queryKey: ["listMeta", entity],
    queryFn: ({ signal }) => fetchListMeta(entity, signal),
    // The filter fields only change with a release, not while the user works.
    staleTime: Infinity,
  });
}
