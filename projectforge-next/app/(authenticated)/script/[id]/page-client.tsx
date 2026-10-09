"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { SCRIPT_PAGE } from "@/components/features/script/script.page";
import { SCRIPT_ROUTE } from "@/components/features/script/script-routes";
import { ScriptExecuteView } from "@/components/features/script/script-execute-view";
import { useScriptEditOutcome } from "@/components/features/script/use-script-edit-outcome";

/**
 * `/script/{id}` executes a stored script — what a row of the list opens —, `/script/new` is the form of
 * a new one: one dynamic segment serves both under the static export. The form of a stored script is
 * `/script/{id}/edit`.
 */
export function ScriptPageClient() {
  const outcome = useScriptEditOutcome();
  const raw = useRouteParams<{ id: string }>("/script/[id]")?.id;
  if (raw === undefined) return null;
  if (raw === "new") {
    return (
      <PageShell>
        <EntityEditPage page={SCRIPT_PAGE} id={null} outcome={outcome} />
      </PageShell>
    );
  }
  const id = Number(raw);
  if (!Number.isInteger(id) || id <= 0) notFound();
  return (
    <ScriptExecuteView
      endpoint="scriptExecute"
      id={id}
      backRoute={SCRIPT_ROUTE}
    />
  );
}
