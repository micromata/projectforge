"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";
import type { EventClickArg } from "@fullcalendar/core";
import { toast } from "@/lib/toast";
import { resolveMenuUrl, sanitizeRedirectUrl } from "@/lib/menu-url";
import { fetchCalendarAction } from "@/lib/rs/calendar";
import type { CalendarActionParams } from "@/lib/rs/calendar-types";
import { parseCalendarEditTarget } from "./calendar-edit-target";
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
    case "teamEvent": {
      // A click on an existing event edits it by its database id; save and cancel return to the calendar
      // (see TEAM_EVENT_PAGE.returnTargets). The numeric `dbId`, never the `uid`: the edit route loads by
      // a numeric id, and a subscribed calendar's `{calId}-{uid}` events are read-only anyway.
      const dbId = event.extendedProps.dbId;
      if (dbId == null) return null;
      // A recurring event's click carries the clicked occurrence's span, so a single/future edit knows
      // which day it acts on — the master is loaded by id, the occurrence threaded in as a prefill (see
      // parseCalendarEditTarget). A one-off event carries nothing extra.
      if (!event.extendedProps.recurrence) return `/teamEvent/${dbId}`;
      const params = new URLSearchParams();
      if (start != null) params.set("startDate", String(start));
      if (end != null) params.set("endDate", String(end));
      if (event.allDay) params.set("allDay", "true");
      const q = params.toString();
      return q ? `/teamEvent/${dbId}?${q}` : `/teamEvent/${dbId}`;
    }
    case "vacation":
      return id != null
        ? `/vacation/edit/${id}?returnToCaller=${RETURN_TO_CALENDAR}`
        : null;
    case "address":
      // A birthday opens the address view. That page isn't migrated yet, so keep it on the legacy
      // React app (`react/` prefix → external, full page load); without it `resolveMenuUrl` would
      // treat the path as this app's own route and land on the unfinished Next address page. The
      // backend builds the same `react/addressView/dynamic/{id}` for the unmigrated category.
      return id != null
        ? `/react/addressView/dynamic/${id}?returnToCaller=${RETURN_TO_CALENDAR}`
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

  // A timesheet or a team event edits in place, everything else navigates. The in-place edit is a
  // nested route of the calendar (`/calendar/timesheet/5`, see calendar/[...edit]): the url is already
  // this app's own `/timesheet/…` shape, and the `/calendar` prefix turns it into that route, so it
  // opens over the still-mounted calendar and a reload or a shared link reopens it.
  const openTarget = useCallback(
    (url: string | undefined | null) => {
      if (!url) return;
      const target = parseCalendarEditTarget(url);
      if (!target) {
        navigate(url);
        return;
      }
      router.push(`/calendar${url}`);
    },
    [navigate, router]
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
