"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useInitialList } from "@/hooks/use-initial-list";
import type { InitialListData, MagicFilter } from "@/lib/rs/types";

/**
 * The filter a list page was last used with. The backend stores it per user and
 * category on every list call (AbstractPagesRest.getList → saveCurrentFilter) and
 * returns it with `initialList`, so it survives a reload and follows the user
 * across devices — the same filter the legacy list page would restore.
 *
 * `isPending` matters: the values seed React state, which can't be swapped in
 * later without overwriting what the user has typed meanwhile, so the caller has
 * to hold the list back until this has arrived.
 */
export function useRememberedFilter(entity: string) {
  const query = useInitialList(entity);
  return { filter: query.data?.filter, isPending: query.isPending };
}

/**
 * Keeps the remembered filter in the `initialList` cache up to date with the
 * filter the list is actually using.
 *
 * The backend does the same on its side, but the cache entry is held forever
 * (the layout in it only changes with a release), so without this, leaving the
 * page and coming back would restore the filter as of the first page load.
 */
export function useRememberFilter(entity: string, filter: MagicFilter) {
  const queryClient = useQueryClient();
  const serialized = JSON.stringify(filter);

  useEffect(() => {
    queryClient.setQueryData<InitialListData>(
      ["initialList", entity],
      (previous) =>
        previous
          ? { ...previous, filter: JSON.parse(serialized) as MagicFilter }
          : previous
    );
  }, [queryClient, entity, serialized]);
}
