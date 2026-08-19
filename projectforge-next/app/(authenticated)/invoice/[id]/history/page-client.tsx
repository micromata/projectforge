"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityHistoryPage } from "@/components/shared/edit/entity-history-page";
import { INVOICE_PAGE } from "@/components/features/invoice/invoice.page";

// Reads the id from the URL at runtime rather than from a server-provided route param, so any id
// works under the static export (see page.tsx and use-route-params.ts).
export function InvoiceHistoryPageClient() {
  const raw = useRouteParams<{ id: string }>("/invoice/[id]/history")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  // An invoice that isn't saved yet ("new") has no history to show.
  if (!Number.isFinite(id) || id <= 0) notFound();

  return (
    <PageShell>
      <EntityHistoryPage page={INVOICE_PAGE} id={id} />
    </PageShell>
  );
}
