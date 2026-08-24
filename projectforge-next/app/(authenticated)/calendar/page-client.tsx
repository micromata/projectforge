"use client";

import { PageShell } from "@/components/shared/page-shell";
import { CalendarPage } from "@/components/features/calendar/calendar-page";

/**
 * The calendar route (`/next/calendar`), the default page after login. A concrete route rather than a
 * category of the generic list page: the calendar is served by its own controllers (`/rs/calendar`) and
 * is hand-built, not laid out by the backend (see MIGRATION-calendar.md).
 */
export function CalendarPageClient() {
  return (
    <PageShell>
      <CalendarPage />
    </PageShell>
  );
}
