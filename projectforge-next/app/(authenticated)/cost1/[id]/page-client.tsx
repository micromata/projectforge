"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { COST1_PAGE } from "@/components/features/cost1/cost1.page";

// Reads the id from the URL at runtime rather than from a server-provided route param, so any id
// works under the static export (see page.tsx and use-route-params.ts).
export function Cost1EditPageClient() {
  const raw = useRouteParams<{ id: string }>("/cost1/[id]")?.id;
  // No match means the URL is not (yet) this route — render nothing rather than a 404, which the
  // pattern's own route can never legitimately show.
  if (raw === undefined) return null;
  // "new" adds a cost unit — the same form, just without an id to load.
  const isNew = raw === "new";
  const id = Number(raw);
  if (!isNew && (!Number.isFinite(id) || id <= 0)) notFound();

  return (
    <PageShell>
      <EntityEditPage page={COST1_PAGE} id={isNew ? null : id} />
    </PageShell>
  );
}
