"use client";

import type { ReactNode } from "react";
import { PageShell } from "@/components/shared/page-shell";
import { CalendarPage } from "@/components/features/calendar/calendar-page";

/**
 * The calendar chrome that stays mounted while an edit hangs off it. Rendered by the calendar `layout`
 * rather than a page, so navigating to a nested `/calendar/timesheet/5` swaps only `children` (the edit
 * modal) and never remounts the calendar — the App Router equivalent of the legacy `<Outlet>` the old
 * React calendar used (see MIGRATION-calendar.md).
 *
 * The edit ([EntityEditModal] under calendar/[...edit]) is a portalled dialog, so where it sits among
 * `children` here does not affect the layout: it always overlays the calendar.
 */
export function CalendarShell({ children }: { children: ReactNode }) {
  return (
    <PageShell>
      <CalendarPage />
      {children}
    </PageShell>
  );
}
