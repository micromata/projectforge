"use client";

import { useEffect, useRef } from "react";
import type { ColumnState } from "./use-table-state";

const DEBOUNCE_MS = 500;

/**
 * Persists the column state to the backend (AbstractPagesRest's setColumnStates,
 * stored in user prefs per entity category).
 *
 * Skips the initial render: the state has just been restored from the server, so
 * posting it straight back would be a pointless write on every page view. Pending
 * writes are flushed on unmount so a resize right before navigating isn't lost.
 */
export function useColumnStatePersistence(
  url: string | undefined,
  state: ColumnState
) {
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isFirstRun = useRef(true);
  // Read inside the timeout so a flush always sends the newest state.
  const latest = useRef(state);

  const serialized = JSON.stringify(state);

  useEffect(() => {
    latest.current = state;
  }, [state]);

  useEffect(() => {
    if (!url) return;
    if (isFirstRun.current) {
      isFirstRun.current = false;
      return;
    }

    const post = () => {
      timer.current = null;
      void fetch(url, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(latest.current),
        keepalive: true,
      }).catch(() => {
        // Losing a column-layout preference must never surface as a user error.
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
