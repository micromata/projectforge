"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { CONTRACT_PAGE } from "@/components/features/contract/contract.page";

// Reads the contract id from the URL at runtime rather than from a server-provided route param, so any
// id works under the static export (see page.tsx and use-route-params.ts).
export function ContractEditPageClient() {
  const raw = useRouteParams<{ id: string }>("/contract/[id]")?.id;
  if (raw === undefined) return null;
  // "new" adds a contract — the same form, just without an id to load.
  const isNew = raw === "new";
  const id = Number(raw);
  if (!isNew && (!Number.isFinite(id) || id <= 0)) notFound();

  return (
    <PageShell>
      <EntityEditPage page={CONTRACT_PAGE} id={isNew ? null : id} />
    </PageShell>
  );
}
