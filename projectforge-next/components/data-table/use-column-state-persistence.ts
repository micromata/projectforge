"use client";

import { useEffect, useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  fetchColumnStates,
  saveColumnStates,
  type ColumnStateDto,
} from "@/lib/rs/client";
import type { ColumnState } from "./use-table-state";

const DEBOUNCE_MS = 500;

/**
 * Loads the column state the backend keeps per entity category (user prefs).
 *
 * `isPending` matters to the caller: applying the state only once it has arrived
 * avoids a visible jump from default layout to stored layout.
 */
export function useStoredColumnState(entity: string | undefined) {
  return useQuery<ColumnStateDto>({
    queryKey: ["columnStates", entity],
    queryFn: ({ signal }) => fetchColumnStates(entity!, signal),
    enabled: !!entity,
    // A layout preference is worth one request per session, not per mount.
    staleTime: Infinity,
    retry: false,
  });
}

/**
 * Persists the column state back to the backend, debounced.
 *
 * Only mount this once the stored state has been read (see useStoredColumnState),
 * otherwise the first render writes defaults over it. Pending writes are flushed
 * on unmount so a resize right before navigating isn't lost.
 */
export function useColumnStatePersistence(
  entity: string | undefined,
  state: ColumnState
) {
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  // Read inside the timeout so a flush always sends the newest state.
  const latest = useRef(state);

  const serialized = JSON.stringify(state);

  useEffect(() => {
    latest.current = state;
  }, [state]);

  useEffect(() => {
    if (!entity) return;

    const post = () => {
      timer.current = null;
      void saveColumnStates(entity, latest.current).catch(() => {
        // Losing a layout preference must never surface as a user error.
      });
    };

    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(post, DEBOUNCE_MS);

    return () => {
      if (timer.current) {
        clearTimeout(timer.current);
        post();
      }
    };
  }, [entity, serialized]);
}
