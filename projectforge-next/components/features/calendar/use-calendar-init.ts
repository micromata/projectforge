"use client";

import { useCallback } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { fetchCalendarInit } from "@/lib/rs/calendar";
import type {
  CalendarFilter,
  CalendarInit,
  CalendarInitPatch,
  StyledTeamCalendar,
  UserRef,
} from "@/lib/rs/calendar-types";

export const CALENDAR_INIT_KEY = ["calendar", "init"] as const;
/** Prefix shared by every events query, so a mutation can invalidate them all at once. */
export const CALENDAR_EVENTS_KEY = ["calendar", "events"] as const;

/** The whole page state. Cached for 30 s — nothing but the user's own mutations changes it. */
export function useCalendarInit() {
  return useQuery({
    queryKey: CALENDAR_INIT_KEY,
    queryFn: ({ signal }) => fetchCalendarInit(signal),
    staleTime: 30_000,
  });
}

/**
 * Everything the `change*` and favourite hooks share: shallow-merge a patch over the cached init
 * (replacing the legacy `saveUpdateResponseInState`), invalidate the events, patch the filter locally,
 * and a `run` wrapper that applies a returned patch and reports failures as a toast.
 *
 * The shallow merge is enough because Spring serialises with `JsonInclude.NON_NULL`: an unchanged key
 * is simply absent, so the spread never overwrites a real value with `null` — unlike the legacy `||`,
 * which lost every `false`/`0` and needed a special case for `isFilterModified`.
 */
export function useInitPatchRunner() {
  const queryClient = useQueryClient();

  const applyPatch = useCallback(
    (patch: CalendarInitPatch | CalendarInit) => {
      queryClient.setQueryData<CalendarInit>(CALENDAR_INIT_KEY, (prev) =>
        prev ? { ...prev, ...patch } : (patch as CalendarInit)
      );
    },
    [queryClient]
  );

  const invalidateEvents = useCallback(
    () => queryClient.invalidateQueries({ queryKey: CALENDAR_EVENTS_KEY }),
    [queryClient]
  );

  /**
   * Patches the cached filter in place, for the endpoints that answer with only `isFilterModified`
   * (`changeTimesheetUser`, `changeShowBreaks`, the vacation selects): the new value has to reach the
   * events key at once, without waiting for the round-trip.
   */
  const patchFilter = useCallback(
    (partial: Partial<CalendarFilter>) => {
      queryClient.setQueryData<CalendarInit>(CALENDAR_INIT_KEY, (prev) =>
        prev?.filter
          ? { ...prev, filter: { ...prev.filter, ...partial } }
          : prev
      );
    },
    [queryClient]
  );

  /**
   * Keeps the resolved timesheet user in sync with `filter.timesheetUserId`. The backend answers
   * `changeTimesheetUser` with only `isFilterModified`, so without this the combobox trigger — which
   * shows `timesheetUser.displayName` — would keep naming the originally-loaded user after a new one
   * is picked. Passed the picked entry (autocomplete) or `null` (cleared / hidden).
   */
  const patchTimesheetUser = useCallback(
    (user: UserRef | null) => {
      queryClient.setQueryData<CalendarInit>(CALENDAR_INIT_KEY, (prev) =>
        prev ? { ...prev, timesheetUser: user } : prev
      );
    },
    [queryClient]
  );

  /**
   * Flips one calendar's `visible` flag in the cached `activeCalendars` at once, so a hide/show shows
   * immediately and does not depend on the server's `activeCalendars` echo — which is derived from the
   * lazily-persisted `calendarIds` and would be short a just-added, not-yet-persisted calendar (see the
   * `keepActiveCalendars` note on `run`, and `setVisibility` in the mutations hook).
   */
  const setLocalVisibility = useCallback(
    (calendarId: number, visible: boolean) => {
      queryClient.setQueryData<CalendarInit>(CALENDAR_INIT_KEY, (prev) =>
        prev?.activeCalendars
          ? {
              ...prev,
              activeCalendars: prev.activeCalendars.map(
                (c): StyledTeamCalendar =>
                  c.id === calendarId ? { ...c, visible } : c
              ),
            }
          : prev
      );
    },
    [queryClient]
  );

  const run = useCallback(
    async (
      promise: Promise<CalendarInitPatch>,
      opts?: { events?: boolean; keepActiveCalendars?: boolean }
    ): Promise<void> => {
      try {
        const patch = await promise;
        // `keepActiveCalendars` drops the server's `activeCalendars` from the patch: it is derived from
        // the lazily-persisted `calendarIds` and would be short (or empty) right after a local add,
        // wiping pills. The caller keeps its own membership (already flipped via `setLocalVisibility`).
        if (opts?.keepActiveCalendars && patch && "activeCalendars" in patch) {
          const rest = { ...patch };
          delete rest.activeCalendars;
          applyPatch(rest);
        } else {
          applyPatch(patch);
        }
        if (opts?.events) await invalidateEvents();
      } catch (err) {
        toast.error(
          err instanceof Error ? err.message : "Calendar update failed."
        );
      }
    },
    [applyPatch, invalidateEvents]
  );

  return {
    applyPatch,
    invalidateEvents,
    patchFilter,
    patchTimesheetUser,
    setLocalVisibility,
    run,
  };
}
