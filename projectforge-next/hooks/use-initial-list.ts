"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchInitialList } from "@/lib/rs/client";
import type { InitialListData } from "@/lib/rs/types";

/**
 * The list page state the backend serves for an entity: the layout (title,
 * actions, the `searchFilter` container describing the available filter fields)
 * plus the user's saved filters.
 *
 * Hand-built pages don't render this layout, but they still need the parts of it
 * that are derived per entity — the filter fields come from the DAO's search
 * fields, so they can't be declared in the frontend.
 *
 * The saved-filter list is patched into this cache entry when a favorite is
 * created, renamed or deleted (see useFilterFavorites), so there is one source of
 * truth for it.
 */
export function useInitialList(entity: string) {
  return useQuery<InitialListData>({
    queryKey: ["initialList", entity],
    queryFn: ({ signal }) => fetchInitialList(entity, signal),
    // The layout only changes with a release, not while the user works.
    staleTime: Infinity,
  });
}
