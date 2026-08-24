"use client";

import { useCallback, useMemo } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  changeCalendarStyle,
  changeDefaultCalendar,
  changeFirstHour,
  changeGridSize,
  changeShowBreaks,
  changeTimesheetUser,
  changeVacationGroups,
  changeVacationUsers,
  refreshSubscriptions,
  setCalendarVisibility,
} from "@/lib/rs/calendar";
import type { CalendarInit, StyledTeamCalendar } from "@/lib/rs/calendar-types";
import { CALENDAR_INIT_KEY, useInitPatchRunner } from "./use-calendar-init";

/**
 * Every `change*` mutation the toolbar and settings dialog trigger, each wired to the effect the
 * backend's answer calls for (see the trigger→effect table in MIGRATION-calendar.md):
 *
 * - style / visibility carry colour and visibility in their payload → apply the patch and refetch events;
 * - timesheet-user / show-breaks / vacations answer with only `isFilterModified`, so the new value is
 *   patched into the filter locally and the events refetch off the changed key;
 * - grid size / first hour / default calendar answer with only `isFilterModified` too and are purely
 *   presentational, so the new value is patched into the filter locally (nothing echoes it back, and
 *   without the local patch the change would not show until a reload) and no refetch is needed.
 *
 * Show-breaks sends `checked` (the new value), fixing the legacy bug where the old value was sent to the
 * server while the client kept the new one.
 */
export function useCalendarFilterMutations() {
  const queryClient = useQueryClient();
  const { invalidateEvents, patchFilter, run } = useInitPatchRunner();

  /** Local add/remove/replace of the chosen calendars (the ± of the calendar select). Persisted via `storeState`. */
  const setActiveCalendars = useCallback(
    (activeCalendars: StyledTeamCalendar[]) => {
      queryClient.setQueryData<CalendarInit>(CALENDAR_INIT_KEY, (prev) =>
        prev ? { ...prev, activeCalendars, isFilterModified: true } : prev
      );
    },
    [queryClient]
  );

  return useMemo(
    () => ({
      changeStyle: (calendarId: number, bgColor: string | undefined) =>
        run(changeCalendarStyle(calendarId, bgColor), { events: true }),
      setVisibility: (calendarId: number, visible: boolean) =>
        run(setCalendarVisibility(calendarId, visible), { events: true }),
      changeGridSize: (size: number) => {
        patchFilter({ gridSize: size });
        return run(changeGridSize(size));
      },
      changeFirstHour: (hour: number) => {
        patchFilter({ firstHour: hour });
        return run(changeFirstHour(hour));
      },
      changeDefaultCalendar: (id: number) => {
        patchFilter({ defaultCalendarId: id });
        return run(changeDefaultCalendar(id));
      },
      changeTimesheetUser: (userId: number | undefined) => {
        patchFilter({ timesheetUserId: userId ?? null });
        return run(changeTimesheetUser(userId), { events: true });
      },
      changeShowBreaks: (checked: boolean) => {
        patchFilter({ showBreaks: checked });
        return run(changeShowBreaks(checked), { events: true });
      },
      changeVacationGroups: (groupIds: number[]) => {
        patchFilter({ vacationGroupIds: groupIds });
        return run(changeVacationGroups(groupIds), { events: true });
      },
      changeVacationUsers: (userIds: number[]) => {
        patchFilter({ vacationUserIds: userIds });
        return run(changeVacationUsers(userIds), { events: true });
      },
      setActiveCalendars,
      /** Re-reads external subscriptions and drops the whole calendar cache, in place of a page reload. */
      refresh: async () => {
        try {
          await refreshSubscriptions();
          await queryClient.invalidateQueries({ queryKey: ["calendar"] });
        } catch {
          await invalidateEvents();
        }
      },
    }),
    [run, patchFilter, setActiveCalendars, invalidateEvents, queryClient]
  );
}
