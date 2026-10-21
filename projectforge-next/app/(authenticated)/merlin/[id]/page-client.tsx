"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { MERLIN_PAGE } from "@/components/features/merlin/merlin.page";
import { MerlinExecuteView } from "@/components/features/merlin/merlin-execute-view";
import { useMerlinEditOutcome } from "@/components/features/merlin/use-merlin-edit-outcome";

/**
 * `/merlin/{id}` executes a template — what a row of the list opens —, `/merlin/new` is the form of a new
 * one: one dynamic segment serves both under the static export. The form of a stored template is
 * `/merlin/{id}/edit`.
 */
export function MerlinPageClient() {
  const outcome = useMerlinEditOutcome(true);
  const raw = useRouteParams<{ id: string }>("/merlin/[id]")?.id;
  if (raw === undefined) return null;
  if (raw === "new") {
    return (
      <PageShell>
        <EntityEditPage page={MERLIN_PAGE} id={null} outcome={outcome} />
      </PageShell>
    );
  }
  const id = Number(raw);
  if (!Number.isInteger(id) || id <= 0) notFound();
  return <MerlinExecuteView id={id} />;
}
