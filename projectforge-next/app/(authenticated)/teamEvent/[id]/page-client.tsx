"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { TEAM_EVENT_PAGE } from "@/components/features/teamEvent/teamEvent.page";

// Reads the event id from the URL at runtime rather than from a server-provided route param, so any id
// works under the static export (see page.tsx and use-route-params.ts).
export function TeamEventEditPageClient() {
  const raw = useRouteParams<{ id: string }>("/teamEvent/[id]")?.id;
  // No match means the URL is not (yet) this route — render nothing rather than a 404, which the
  // pattern's own route can never legitimately show.
  if (raw === undefined) return null;
  // "new" adds an event — the same form, just without an id to load (the calendar presets it via the URL).
  const isNew = raw === "new";
  const id = Number(raw);
  if (!isNew && (!Number.isFinite(id) || id <= 0)) notFound();

  return (
    <PageShell>
      <EntityEditPage page={TEAM_EVENT_PAGE} id={isNew ? null : id} />
    </PageShell>
  );
}
