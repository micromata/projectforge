"use client";

import { useEffect, useRef } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  fetchDashboardLayout,
  saveDashboardLayout,
  type DashboardLayout,
} from "@/lib/rs/ui-settings";

const DEBOUNCE_MS = 500;

const queryKey = (id: string) => ["dashboardLayout", id];

/**
 * The layout the user stored for dashboard `id`. Read once per session (`staleTime: Infinity`); a failed
 * read leaves the dashboard on its defaults rather than blocking the charts.
 */
export function useStoredDashboardLayout(id: string) {
  return useQuery<DashboardLayout>({
    queryKey: queryKey(id),
    queryFn: ({ signal }) => fetchDashboardLayout(id, signal),
    staleTime: Infinity,
    retry: false,
  });
}

/**
 * Persists `layout` for dashboard `id`, debounced, as the list's column state is (see
 * useColumnStatePersistenceByUrl): only once it differs from what was read, flushed on unmount so a change
 * right before navigating isn't lost. The cached copy follows every change, so a later mount in the same
 * session starts from the newest layout rather than the one of the first read.
 */
export function useDashboardLayoutPersistence(
  id: string,
  layout: DashboardLayout
) {
  const queryClient = useQueryClient();
  const serialized = JSON.stringify(layout);
  // What the server is known to hold; nothing to write while the layout equals it.
  const persisted = useRef(serialized);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const latest = useRef(serialized);

  // Sends the newest layout unless the server already holds it. A ref, so the unmount flush below
  // reaches the current dashboard id.
  const post = useRef(() => {});
  useEffect(() => {
    post.current = () => {
      timer.current = null;
      const value = latest.current;
      if (value === persisted.current) return;
      persisted.current = value;
      void saveDashboardLayout(id, JSON.parse(value) as DashboardLayout).catch(
        () => {
          // Losing a layout preference must never surface as a user error.
        }
      );
    };
  }, [id]);

  useEffect(() => {
    latest.current = serialized;
    queryClient.setQueryData<DashboardLayout>(
      queryKey(id),
      JSON.parse(serialized) as DashboardLayout
    );
    if (serialized === persisted.current) return;
    // Restarted by every change, so a series of edits (dragging, a few size clicks) ends in one write.
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => post.current(), DEBOUNCE_MS);
  }, [queryClient, id, serialized]);

  // Flushes a pending write on unmount only, not on every change (which would defeat the debounce).
  useEffect(
    () => () => {
      if (timer.current) {
        clearTimeout(timer.current);
        post.current();
      }
    },
    []
  );
}
