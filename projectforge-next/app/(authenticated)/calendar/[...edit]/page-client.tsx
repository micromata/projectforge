"use client";

import { useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useRouteParams } from "@/hooks/use-route-params";
import { useCurrentUserRef } from "@/hooks/use-current-user-ref";
import { EntityEditModal } from "@/components/shared/edit/entity-edit-modal";
import { TAB_PARAM } from "@/components/shared/edit-page-tabs";
import { parseCalendarEditTarget } from "@/components/features/calendar/calendar-edit-target";
import { TIMESHEET_PAGE } from "@/components/features/timesheet/timesheet.page";
import {
  CALENDAR_EVENTS_KEY,
  CALENDAR_INIT_KEY,
} from "@/components/features/calendar/use-calendar-init";
import { useCalendarFilterMutations } from "@/components/features/calendar/use-calendar-filter-mutations";
import type { CalendarInit } from "@/lib/rs/calendar-types";

/**
 * The edit that hangs off the calendar as a nested route (`/calendar/timesheet/5`,
 * `/calendar/timesheet/new?startDate=…`, `/calendar/teamEvent/7`): a timesheet or a team event opened
 * over the still-mounted calendar (see CalendarShell), URL-driven so a reload or a shared link reopens
 * it. The calendar builds these urls in useCalendarAction by prefixing its own `/timesheet/…` shapes
 * with `/calendar`; here the segments are read back into the same target `parseCalendarEditTarget`
 * yields, so both ends share one entity registry.
 */
export function CalendarEditRouteClient() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const params = useRouteParams<{ edit: string[] }>("/calendar/[...edit]");
  const searchParams = useSearchParams();
  const mutations = useCalendarFilterMutations();
  const currentUser = useCurrentUserRef();

  // Every way the modal closes — save, cancel, delete, ESC, overlay, an entry gone — funnels through
  // onOpenChange(false). Both refetch and navigation belong here so the calendar redraws the edited
  // sheet (invalidateEntity refreshes the entity's caches, not the calendar's ["calendar","events"])
  // and the url returns to the calendar itself.
  const close = useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: CALENDAR_EVENTS_KEY });
    router.push("/calendar");
  }, [queryClient, router]);

  // A new colleague logs their first timesheet and doesn't see it, because the calendar hides
  // timesheets by default. Saving one here turns their own timesheets on — but only when nothing is
  // shown yet, so a privileged user already viewing someone else's timesheets is left alone. The
  // logged-in user's real id is passed (not the show/hide sentinel), which is what changeTimesheetUser
  // wants for a privileged account; a normal user is clamped to themselves server-side regardless.
  const activateOwnTimesheets = useCallback(() => {
    const init = queryClient.getQueryData<CalendarInit>(CALENDAR_INIT_KEY);
    const userId = currentUser?.id;
    if (userId == null) return;
    if ((init?.timesheetUser?.id ?? 0) > 0) return;
    void mutations.changeTimesheetUser(userId);
  }, [queryClient, currentUser, mutations]);

  // No match during the prerender pass, and for the instant a client navigation is still on the old
  // url — render nothing over the calendar rather than a stray modal.
  const segments = params?.edit;
  if (!segments) return null;

  const query = searchParams.toString();
  const url = `/${segments.join("/")}${query ? `?${query}` : ""}`;
  const target = parseCalendarEditTarget(url);
  // A path the calendar does not edit in place (anything but a timesheet or team event) never reaches
  // this route, but if it does it is not ours to open.
  if (!target) return null;

  return (
    <EntityEditModal
      // A fresh mount per entry, so switching from one sheet to another never reuses the first's form.
      key={`${target.page.entity}:${target.id ?? "new"}`}
      page={target.page}
      id={target.id}
      newParams={target.newParams}
      // A recurring team event opens on the clicked occurrence, layered over the loaded master without
      // dirtying the form; a drag or resize opens the entry already at its moved position as a change
      // to save (see calendar-edit-target). Both undefined for a plain click.
      prefill={target.prefill}
      dirtyPrefill={target.dirtyPrefill}
      // `?tab=history` on the link opens that tab straight away (see EntityEditDialogShell).
      initialTab={searchParams.get(TAB_PARAM) ?? undefined}
      open
      onOpenChange={(next) => {
        if (!next) close();
      }}
      // Saving a timesheet from the calendar activates the user's own timesheets when none are shown,
      // so the sheet they just booked is actually visible (see activateOwnTimesheets).
      onSaved={
        target.page === TIMESHEET_PAGE ? activateOwnTimesheets : undefined
      }
      // A clone or a convert stays in the calendar: open the prepared new entry as the calendar's own
      // nested route (`/calendar/timesheet/new?clone=1`, `/calendar/teamEvent/new?clone=1`), so it
      // layers over the still-mounted calendar in this dialog rather than leaving for the full page. The
      // handover survives the navigation (see usePendingClone); the changed key remounts the form fresh.
      onCloneNavigate={(route) => router.push(`/calendar${route}`)}
    />
  );
}
