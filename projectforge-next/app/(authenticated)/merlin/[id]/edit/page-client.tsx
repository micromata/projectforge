"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { MERLIN_PAGE } from "@/components/features/merlin/merlin.page";
import { useMerlinEditOutcome } from "@/components/features/merlin/use-merlin-edit-outcome";

/** The admin form of a stored template: its settings, variables, access and attachments. */
export function MerlinEditPageClient() {
  const outcome = useMerlinEditOutcome(false);
  const raw = useRouteParams<{ id: string }>("/merlin/[id]/edit")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  if (!Number.isInteger(id) || id <= 0) notFound();
  return (
    <PageShell>
      <EntityEditPage page={MERLIN_PAGE} id={id} outcome={outcome} />
    </PageShell>
  );
}
