"use client";

import { useCallback, useEffect, useRef } from "react";
import { storeCalendarState } from "@/lib/rs/calendar";
import type { CalendarState } from "@/lib/rs/calendar-types";

/**
 * Persists the last view / date / active-calendars so the page reopens where the user left it.
 *
 * Debounced (300 ms by default): `datesSet` fires for every prev/next step and a fast double
 * navigation would otherwise persist an intermediate date. Only the most recent state is sent, and a
 * failure is swallowed — a missed "remember where I was" is not worth an interruption.
 */
export function useStoreCalendarState(delay = 300) {
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined);
  const latest = useRef<CalendarState>(undefined);

  useEffect(
    () => () => {
      if (timer.current) clearTimeout(timer.current);
    },
    []
  );

  return useCallback(
    (state: CalendarState) => {
      latest.current = state;
      if (timer.current) clearTimeout(timer.current);
      timer.current = setTimeout(() => {
        if (latest.current)
          void storeCalendarState(latest.current).catch(() => {});
      }, delay);
    },
    [delay]
  );
}
