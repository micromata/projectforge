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
export function useListMeta(
  entity: string,
  /**
   * Force a fresh fetch on every mount of this observer. The remembered filter lives in this same
   * cache entry, and the backend updates it on every list call — but the entry is kept forever
   * (`staleTime: Infinity`) and held alive across the edit round-trip by the edit page's own
   * observers, so a long-lived tab would otherwise keep seeding a days-old filter it once loaded.
   * The list page's filter observer (useRememberedFilter) sets this so returning to a list re-reads
   * the server's last-used filter; the other consumers (filter fields, access flags, edit targets)
   * leave it off and simply pick up whatever that refetch produced.
   */
  { alwaysFresh = false }: { alwaysFresh?: boolean } = {}
) {
  return useQuery<ListMetaData>({
    queryKey: ["listMeta", entity],
    queryFn: ({ signal }) => fetchListMeta(entity, signal),
    // The filter fields only change with a release, not while the user works.
    staleTime: Infinity,
    // But the remembered filter does change while the user works, so its observer overrides the
    // stale time per mount rather than trusting the cached copy (see the parameter above).
    refetchOnMount: alwaysFresh ? "always" : undefined,
  });
}
