"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { TIMESHEET_PAGE } from "@/components/features/timesheet/timesheet.page";

// Reads the time sheet id from the URL at runtime rather than from a server-provided route param, so any
// id works under the static export (see page.tsx and use-route-params.ts).
export function TimesheetEditPageClient() {
  const raw = useRouteParams<{ id: string }>("/timesheet/[id]")?.id;
  // No match means the URL is not (yet) this route — render nothing rather than a 404, which the
  // pattern's own route can never legitimately show.
  if (raw === undefined) return null;
  // "new" adds a sheet — the same form, just without an id to load (the calendar presets it via the URL).
  const isNew = raw === "new";
  const id = Number(raw);
  if (!isNew && (!Number.isFinite(id) || id <= 0)) notFound();

  return (
    <PageShell>
      <EntityEditPage page={TIMESHEET_PAGE} id={isNew ? null : id} />
    </PageShell>
  );
}
