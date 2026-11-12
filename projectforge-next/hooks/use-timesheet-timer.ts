"use client";

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  TIMESHEET_TIMER_QUERY_KEY,
  clearTimesheetTimer,
  fetchTimesheetTimer,
  startTimesheetTimer,
  type TimesheetTimer,
} from "@/lib/rs/timesheet-timer";

/**
 * The timer of the user, kept at the server and shared with the app. Refetches when the tab regains focus, so a
 * timer started or stopped in the app shows up; no polling (every call touches the HTTP session).
 */
export function useTimesheetTimer(enabled = true) {
  const queryClient = useQueryClient();
  const query = useQuery<TimesheetTimer>({
    queryKey: TIMESHEET_TIMER_QUERY_KEY,
    queryFn: ({ signal }) => fetchTimesheetTimer(signal),
    enabled,
    retry: false,
    // Stale at once, so every return to the tab asks the server: the app may have started or stopped it.
    staleTime: 0,
    refetchOnWindowFocus: true,
  });
  const store = (timer: TimesheetTimer) =>
    queryClient.setQueryData(TIMESHEET_TIMER_QUERY_KEY, timer);
  const start = useMutation({
    mutationFn: startTimesheetTimer,
    onSuccess: store,
  });
  const clear = useMutation({
    mutationFn: clearTimesheetTimer,
    onSuccess: store,
  });
  const timer = query.data?.running ? query.data : null;
  // The server's clock minus ours (taken when the answer came), so a wrong clock of this device does not matter.
  const offset = query.data?.clockOffset ?? 0;
  return {
    timer,
    isLoading: query.isLoading,
    start: () => start.mutate(),
    discard: () => clear.mutate(),
    discardAsync: () => clear.mutateAsync(),
    isBusy: start.isPending || clear.isPending,
    /** The server's time now in ms. */
    serverNow: () => Date.now() + offset,
  };
}

/** Re-renders every [intervalMs] while [active], for the shown elapsed time. */
export function useTick(active: boolean, intervalMs = 15_000): number {
  const [tick, setTick] = useState(0);
  useEffect(() => {
    if (!active) return;
    const id = window.setInterval(() => setTick((n) => n + 1), intervalMs);
    return () => window.clearInterval(id);
  }, [active, intervalMs]);
  return tick;
}
