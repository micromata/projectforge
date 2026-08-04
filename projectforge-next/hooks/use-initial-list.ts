"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchInitialList } from "@/lib/rs/client";
import type { DynamicPageResponse } from "@/lib/rs/types";

/**
 * The list layout the backend serves for an entity (title, actions, and the
 * `searchFilter` container describing the available filter fields).
 *
 * Hand-built pages don't render this layout, but they still need the parts of it
 * that are derived per entity — the filter fields come from the DAO's search
 * fields, so they can't be declared in the frontend.
 */
export function useInitialList(entity: string) {
  return useQuery<DynamicPageResponse>({
    queryKey: ["initialList", entity],
    queryFn: ({ signal }) => fetchInitialList(entity, signal),
    // The layout only changes with a release, not while the user works.
    staleTime: Infinity,
  });
}
