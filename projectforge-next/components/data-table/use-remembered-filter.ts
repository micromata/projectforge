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
 */
export function useRememberedFilter(entity: string) {
  const query = useListMeta(entity);
  return { filter: query.data?.filter, isPending: query.isPending };
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
