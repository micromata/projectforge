"use client";

import { useEffect, useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  fetchColumnStatesFromUrl,
  saveColumnStatesToUrl,
  type ColumnStateDto,
} from "@/lib/rs/column-state";
import type { ColumnState } from "./use-table-state";

const DEBOUNCE_MS = 500;

/**
 * Loads the column state stored for the user from an explicit URL.
 *
 * `isPending` matters to the caller: applying the state only once it has arrived
 * avoids a visible jump from default layout to stored layout. Without a URL the
 * query stays idle, so a caller can pass one that isn't known yet.
 */
export function useStoredColumnStateByUrl(url: string | undefined) {
  return useQuery<ColumnStateDto>({
    queryKey: ["columnStates", url],
    queryFn: ({ signal }) => fetchColumnStatesFromUrl(url!, signal),
    enabled: !!url,
    // A layout preference is worth one request per session, not per mount.
    staleTime: Infinity,
    retry: false,
  });
}

/** The same, for a list page addressed by its entity category. */
export function useStoredColumnState(entity: string | undefined) {
  return useStoredColumnStateByUrl(
    entity ? `/rs/${entity}/columnStates` : undefined
  );
}

/**
 * Persists the column state to the given URL, debounced.
 *
 * Only mount this once the stored state has been read (see useStoredColumnState),
 * otherwise the first render writes defaults over it. Pending writes are flushed
 * on unmount so a resize right before navigating isn't lost.
 */
export function useColumnStatePersistenceByUrl(
  url: string | undefined,
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
    if (!url) return;

    const post = () => {
      timer.current = null;
      void saveColumnStatesToUrl(url, latest.current).catch(() => {
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
  }, [url, serialized]);
}

/** The same, for a list page addressed by its entity category. */
export function useColumnStatePersistence(
  entity: string | undefined,
  state: ColumnState
) {
  useColumnStatePersistenceByUrl(
    entity ? `/rs/${entity}/setColumnStates` : undefined,
    state
  );
}
