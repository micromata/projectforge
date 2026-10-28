"use client";

import { useCallback, useEffect, useRef } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
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
 * Keeps the cached stored state in step with the state the table is actually using.
 *
 * The read above is cached for the whole session (`staleTime: Infinity`), so what a later mount seeds
 * itself from is that cache entry rather than the server. Without this, leaving the list and coming
 * back would restore the columns and the sort order as of the *first* page load — and a lost sort
 * order also costs the highlight: the row the user just edited then sits on a different page than the
 * one the list opens on, so there is nothing on screen to mark (see useHighlightedRow). The backend
 * has the newest state either way (it is posted below); this is only about the copy in the cache.
 *
 * The counterpart for the filter is `useRememberFilter`, for the same reason.
 */
export function useRememberColumnState(
  entity: string | undefined,
  state: ColumnState
) {
  const queryClient = useQueryClient();
  const serialized = JSON.stringify(state);

  useEffect(() => {
    if (!entity) return;
    queryClient.setQueryData<ColumnStateDto>(
      ["columnStates", `/rs/${entity}/columnStates`],
      // Only for an entry that was read: seeding a missing one from here would let the first render's
      // defaults pass for something the server had stored, and the real read would then be skipped.
      (previous) =>
        previous ? (JSON.parse(serialized) as ColumnStateDto) : previous
    );
  }, [queryClient, entity, serialized]);
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
  // The state the server has (as far as we know): the one the table mounted with, then the last one written.
  // Posting a state equal to it would only touch the user prefs on every page view.
  const persisted = useRef<{ url: string; state: string } | null>(null);

  const serialized = JSON.stringify(state);

  useEffect(() => {
    latest.current = state;
  }, [state]);

  useEffect(() => {
    if (!url) return;
    // Mounted once the stored state has been read, so the first state is the stored one: nothing to write yet.
    if (persisted.current?.url !== url)
      persisted.current = { url, state: serialized };
    if (serialized === persisted.current.state) return;

    const post = () => {
      timer.current = null;
      if (suspended.current) return;
      const body = JSON.stringify(latest.current);
      if (body === persisted.current?.state) return;
      persisted.current = { url, state: body };
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

/**
 * The same, for a list page addressed by its entity category — and it holds the cached copy of the
 * stored state to what it writes, since here both URLs are known (see useRememberColumnState).
 */
export function useColumnStatePersistence(
  entity: string | undefined,
  state: ColumnState
) {
  useRememberColumnState(entity, state);
  return useColumnStatePersistenceByUrl(
    entity ? `/rs/${entity}/setColumnStates` : undefined,
    state
  );
}

const localStorageKey = (name: string) => `pf.columnStates.${name}`;

/**
 * The column state kept in the browser's local storage under `name` — for a page without user prefs to
 * keep it in (the external access of a data transfer area, whose visitor has no ProjectForge account).
 * Read synchronously, so unlike the backend variant there is nothing to wait for; an unreadable entry
 * (private mode, a value of an older shape) counts as none.
 */
export function readLocalColumnState(name: string | undefined): ColumnState {
  if (!name) return {};
  try {
    const stored = window.localStorage.getItem(localStorageKey(name));
    const parsed: unknown = stored ? JSON.parse(stored) : null;
    return parsed && typeof parsed === "object" ? (parsed as ColumnState) : {};
  } catch {
    return {};
  }
}

/**
 * Writes the column state to the local storage under `name` (see readLocalColumnState). Not debounced
 * like the backend variant: a local write costs nothing worth saving, and none can be lost on unmount.
 */
export function useLocalColumnStatePersistence(
  name: string | undefined,
  state: ColumnState
) {
  const serialized = JSON.stringify(state);
  useEffect(() => {
    if (!name) return;
    try {
      window.localStorage.setItem(localStorageKey(name), serialized);
    } catch {
      // Private mode or full storage: the layout just isn't remembered.
    }
  }, [name, serialized]);
}
