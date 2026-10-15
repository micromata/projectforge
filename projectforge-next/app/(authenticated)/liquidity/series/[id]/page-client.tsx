"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { EntityEditPage } from "@/components/shared/edit/entity-edit-page";
import { LIQUIDITY_SERIES_PAGE } from "@/components/features/liquidity/liquidity-series.page";

// Reads the id from the URL at runtime rather than from a server-provided route param, so any id
// works under the static export (see page.tsx and use-route-params.ts).
export function LiquiditySeriesEditPageClient() {
  const raw = useRouteParams<{ id: string }>("/liquidity/series/[id]")?.id;
  // No match means the URL is not (yet) this route — render nothing rather than a 404.
  if (raw === undefined) return null;
  const id = Number(raw);
  // A series is only ever edited, never added here (it is born via the entry form's "repeat" block), so
  // an id that is not a real, positive PK is a 404.
  if (!Number.isFinite(id) || id <= 0) notFound();

  return (
    <PageShell>
      <EntityEditPage page={LIQUIDITY_SERIES_PAGE} id={id} />
    </PageShell>
  );
}
