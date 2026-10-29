"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { CVR_CASE_PAGE } from "@/components/features/cve-radar/cases/case.page";

// Reads the id from the URL at runtime rather than from a server-provided route param, so any id
// works under the static export (see page.tsx and use-route-params.ts).
export function CvrCaseEditPageClient() {
  const raw = useRouteParams<{ id: string }>("/cvrcase/[id]")?.id;
  if (raw === undefined) return null;
  // "new" adds a case — the same form, just without an id to load.
  const isNew = raw === "new";
  const id = Number(raw);
  if (!isNew && (!Number.isFinite(id) || id <= 0)) notFound();

  return (
    <PageShell>
      <EntityEditPage page={CVR_CASE_PAGE} id={isNew ? null : id} />
    </PageShell>
  );
}
