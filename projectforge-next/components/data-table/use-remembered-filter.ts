"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useListMeta } from "@/hooks/use-list-meta";
import type { ListMetaData, MagicFilter } from "@/lib/rs/types";

/**
 * The filter a list page was last used with. The backend stores it per user and
 * category on every list call (AbstractEntityRest.getList → saveCurrentFilter) and
 * returns it with `listMeta`, so it survives a reload and follows the user
 * across devices — the same filter the legacy list page would restore.
 *
 * `isPending` matters: the values seed React state, which can't be swapped in
 * later without overwriting what the user has typed meanwhile, so the caller has
 * to hold the list back until this has arrived.
 *
 * The fetch is forced fresh on every mount (`alwaysFresh`): the cache entry is
 * held forever and kept alive across the edit round-trip, so a long-lived tab
 * would otherwise re-seed a filter it loaded days ago instead of the one the
 * backend last stored. `isFetching` is folded into the gate so that mount refetch
 * settles before the list seeds — the caller shows its spinner meanwhile, exactly
 * as on a first open.
 */
export function useRememberedFilter(
  entity: string,
  {
    /**
     * Off for a view beside a mounted list (the "Grafiken" tabs): it must follow the filter the list is
     * *showing*, which useRememberFilter keeps in the cache. The backend's copy can lag behind it — the
     * list answers a recently used filter from its own cache without posting it, so the backend still
     * holds the one before — and a forced refetch would overwrite the live filter with that stale one.
     * The cache is still fetched when empty.
     */
    fresh = true,
  }: { fresh?: boolean } = {}
) {
  const query = useListMeta(entity, { alwaysFresh: fresh });
  return {
    filter: query.data?.filter,
    isPending: query.isPending || query.isFetching,
  };
}

/**
 * Keeps the remembered filter in the `listMeta` cache up to date with the
 * filter the list is actually using.
 *
 * The backend does the same on its side, but the cache entry is held forever
 * (the filter fields in it only change with a release), so without this, leaving
 * the page and coming back would restore the filter as of the first page load.
 */
export function useRememberFilter(
  entity: string,
  filter: MagicFilter,
  /**
   * Off for a transient jump (`doNotStore`): its filter must not become the remembered one, on the
   * backend or here — otherwise leaving the page and coming back would restore the task filter the
   * consumption bar seeded, exactly what the transient jump avoids.
   */
  enabled = true
) {
  const queryClient = useQueryClient();
  const serialized = JSON.stringify(filter);

  useEffect(() => {
    if (!enabled) return;
    queryClient.setQueryData<ListMetaData>(["listMeta", entity], (previous) =>
      previous
        ? { ...previous, filter: JSON.parse(serialized) as MagicFilter }
        : previous
    );
  }, [queryClient, entity, serialized, enabled]);
}
