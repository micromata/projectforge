"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { SCRIPT_PAGE } from "@/components/features/script/script.page";
import { useScriptEditOutcome } from "@/components/features/script/use-script-edit-outcome";

/** The form of a stored script: its sources, parameters and who may execute it. */
export function ScriptEditPageClient() {
  const outcome = useScriptEditOutcome();
  const raw = useRouteParams<{ id: string }>("/script/[id]/edit")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  if (!Number.isInteger(id) || id <= 0) notFound();
  return (
    <PageShell>
      <EntityEditPage page={SCRIPT_PAGE} id={id} outcome={outcome} />
    </PageShell>
  );
}
