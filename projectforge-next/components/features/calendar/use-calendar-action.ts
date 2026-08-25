"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import type { EventClickArg } from "@fullcalendar/core";
import { toast } from "@/lib/toast";
import { resolveMenuUrl, sanitizeRedirectUrl } from "@/lib/menu-url";
import { fetchCalendarAction } from "@/lib/rs/calendar";
import type { CalendarActionParams } from "@/lib/rs/calendar-types";
import { useEntityEditModalStore } from "@/store/entity-edit-modal-store";
import { parseCalendarEditTarget } from "./calendar-edit-target";
import { CALENDAR_EVENTS_KEY } from "./use-calendar-init";
import { toTeamEventRoute } from "./team-event-route";
import { toTimesheetRoute } from "./timesheet-route";

/** `returnToCaller` for the pages the calendar opens, so their Save/Cancel comes back here. */
const RETURN_TO_CALENDAR = encodeURIComponent("/next/calendar");

/** Epoch seconds, the form the legacy `eventClick` urls use and `TimesheetPagesRest` parses. */
function epochSeconds(date: Date | null): number | undefined {
  return date ? Math.floor(date.getTime() / 1000) : undefined;
}

/**
 * Where a click on an existing event navigates, by `extendedProps.category` (see the routing table in
 * MIGRATION-calendar.md). `timesheet-stats` goes nowhere; a break opens a new timesheet for its span;
 * vacation and birthday open their own views; everything else opens `/<category>/edit/<id>`. The urls
 * are unprefixed frontend paths, so `resolveMenuUrl` maps them onto this app under its base path.
 */
function eventClickUrl(event: EventClickArg["event"]): string | null {
  const category = event.extendedProps.category as string | undefined;
  const id = event.extendedProps.uid ?? event.extendedProps.dbId;
  const start = epochSeconds(event.start);
  const end = epochSeconds(event.end);
  switch (category) {
    case "timesheet-stats":
      return null;
    case "timesheet-break":
      // A gap between two sheets: open a new one for its span (this app's add route, not the legacy
      // `/timesheet/edit`), preset from the break's start and end.
      return `/timesheet/new?startDate=${start ?? ""}&endDate=${end ?? ""}`;
    case "timesheet":
      // A click on an existing sheet edits it by id; save and cancel return to the calendar (see
      // TIMESHEET_PAGE.returnTargets), so no `returnToCaller` is needed here.
      return id != null ? `/timesheet/${id}` : null;
    case "teamEvent":
      // A click on an existing event edits it by its database id; save and cancel return to the calendar
      // (see TEAM_EVENT_PAGE.returnTargets). The numeric `dbId`, never the `uid`: the edit route loads by
      // a numeric id, and a subscribed calendar's `{calId}-{uid}` events are read-only anyway.
      return event.extendedProps.dbId != null
        ? `/teamEvent/${event.extendedProps.dbId}`
        : null;
    case "vacation":
      return id != null
        ? `/vacation/edit/${id}?returnToCaller=${RETURN_TO_CALENDAR}`
        : null;
    case "address":
      return id != null
        ? `/addressView/dynamic/${id}?returnToCaller=${RETURN_TO_CALENDAR}`
        : null;
    default:
      return category && id != null
        ? `/${category}/edit/${id}?startDate=${start ?? ""}&endDate=${end ?? ""}`
        : null;
  }
}

/**
 * The two ways the calendar turns an interaction into a navigation: a click on an existing event (a
 * direct url) and a slot select / create / resize / drag (the `/action` endpoint, which answers with
 * the edit url to open). Both go through `sanitizeRedirectUrl` + `resolveMenuUrl` before `router.push`.
 */
export function useCalendarAction() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const openEntityEdit = useEntityEditModalStore((s) => s.openEntityEdit);

  const navigate = useCallback(
    (url: string | undefined | null) => {
      const safe = sanitizeRedirectUrl(url);
      if (!safe) return;
      const target = resolveMenuUrl(safe);
      if (target.kind === "internal") router.push(target.href);
      else window.location.assign(target.href);
    },
    [router]
  );

  // A timesheet or a team event opens in the modal, everything else navigates. The modal's save and
  // its dismissal both refetch the events: `invalidateEntity` refreshes the entity's own caches but
  // not the calendar's (`["calendar","events"]`), so a sheet edited in place would otherwise stay as
  // it was drawn until the next reload.
  const openTarget = useCallback(
    (url: string | undefined | null) => {
      if (!url) return;
      const target = parseCalendarEditTarget(url);
      if (!target) {
        navigate(url);
        return;
      }
      const refetch = () =>
        void queryClient.invalidateQueries({ queryKey: CALENDAR_EVENTS_KEY });
      openEntityEdit({ ...target, onSaved: refetch, onClose: refetch });
    },
    [navigate, openEntityEdit, queryClient]
  );

  const handleEventClick = useCallback(
    (arg: EventClickArg) => openTarget(eventClickUrl(arg.event)),
    [openTarget]
  );

  const requestAction = useCallback(
    async (params: CalendarActionParams) => {
      try {
        const action = await fetchCalendarAction(params);
        // The backend answers a slot select / create / resize / drag with a legacy `/<category>/edit` url,
        // its category chosen from the calendar filter (`CalendarServicesRest.action`): a timesheet where
        // no default calendar is set, a team event where one is. Each rewrite is total and only touches
        // its own category, so both can wrap the url in turn (see toTimesheetRoute, toTeamEventRoute).
        const url = action.url
          ? toTeamEventRoute(toTimesheetRoute(action.url))
          : action.url;
        openTarget(url);
      } catch (err) {
        toast.error(err instanceof Error ? err.message : "Action failed.");
      }
    },
    [openTarget]
  );

  return { handleEventClick, requestAction };
}
