"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { COST2_TYPE_PAGE } from "@/components/features/cost2-type/cost2-type.page";

// Reads the id from the URL at runtime rather than from a server-provided route param, so any id
// works under the static export (see page.tsx and use-route-params.ts).
export function Cost2TypeEditPageClient() {
  const raw = useRouteParams<{ id: string }>("/cost2Type/[id]")?.id;
  // No match means the URL is not (yet) this route — render nothing rather than a 404, which the
  // pattern's own route can never legitimately show.
  if (raw === undefined) return null;
  // "new" adds a type — the same form, just without an id to load.
  const isNew = raw === "new";
  const id = Number(raw);
  // A cost-2 type's number is a user-assigned key that starts at 0 ("00"), so 0 is a valid id here —
  // only a non-numeric or negative segment is not a route.
  if (!isNew && (!Number.isInteger(id) || id < 0)) notFound();

  return (
    <PageShell>
      <EntityEditPage page={COST2_TYPE_PAGE} id={isNew ? null : id} />
    </PageShell>
  );
}
