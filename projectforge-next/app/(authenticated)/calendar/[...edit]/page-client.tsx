"use client";

import { useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useRouteParams } from "@/hooks/use-route-params";
import { EntityEditModal } from "@/components/shared/edit/entity-edit-modal";
import { TAB_PARAM } from "@/components/shared/edit-page-tabs";
import { parseCalendarEditTarget } from "@/components/features/calendar/calendar-edit-target";
import { CALENDAR_EVENTS_KEY } from "@/components/features/calendar/use-calendar-init";

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

  // Every way the modal closes — save, cancel, delete, ESC, overlay, an entry gone — funnels through
  // onOpenChange(false). Both refetch and navigation belong here so the calendar redraws the edited
  // sheet (invalidateEntity refreshes the entity's caches, not the calendar's ["calendar","events"])
  // and the url returns to the calendar itself.
  const close = useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: CALENDAR_EVENTS_KEY });
    router.push("/calendar");
  }, [queryClient, router]);

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
      // `?tab=history` on the link opens that tab straight away (see EntityEditDialogShell).
      initialTab={searchParams.get(TAB_PARAM) ?? undefined}
      open
      onOpenChange={(next) => {
        if (!next) close();
      }}
    />
  );
}
