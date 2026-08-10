"use client";

import { useCallback, useEffect, useRef } from "react";
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
 *
 * The returned `suspendUntil` exists for "reset columns": that endpoint clears the stored state
 * server-side and answers with the defaults, which the caller then applies. A debounced write still
 * carrying the *pre-reset* state would otherwise land after the reset — the server would have the
 * old columns back, and the next read would restore them (see useGridStateReset).
 */
export function useColumnStatePersistenceByUrl(
  url: string | undefined,
  state: ColumnState
) {
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  // Read inside the timeout so a flush always sends the newest state.
  const latest = useRef(state);
  // While set, writes are dropped: the state they would send is known to be outdated.
  const suspended = useRef<Promise<unknown> | null>(null);

  const serialized = JSON.stringify(state);

  useEffect(() => {
    latest.current = state;
  }, [state]);

  useEffect(() => {
    if (!url) return;

    const post = () => {
      timer.current = null;
      if (suspended.current) return;
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

  /**
   * Drops pending and new writes until `work` has settled, so a state the caller is about to
   * replace can't be written on top of the result.
   */
  return useCallback(async <T>(work: () => Promise<T>): Promise<T> => {
    if (timer.current) {
      clearTimeout(timer.current);
      timer.current = null;
    }
    const running = work();
    suspended.current = running;
    try {
      return await running;
    } finally {
      // Only the newest suspension clears the flag; an older one finishing must not re-enable
      // writes while a later reset is still in flight.
      if (suspended.current === running) suspended.current = null;
    }
  }, []);
}

/** The same, for a list page addressed by its entity category. */
export function useColumnStatePersistence(
  entity: string | undefined,
  state: ColumnState
) {
  return useColumnStatePersistenceByUrl(
    entity ? `/rs/${entity}/setColumnStates` : undefined,
    state
  );
}
