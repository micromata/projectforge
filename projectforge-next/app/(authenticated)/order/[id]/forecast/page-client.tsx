"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { PageShell } from "@/components/shared/page-shell";
import { OrderForecastPage } from "@/components/features/order/forecast/order-forecast-page";

// Reads the id from the URL at runtime rather than from a server-provided route param, so any id
// works under the static export (see page.tsx and use-route-params.ts).
export function OrderForecastPageClient() {
  const raw = useRouteParams<{ id: string }>("/order/[id]/forecast")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  // The analysis is computed over the saved order, so an unsaved one ("new") has nothing to show.
  if (!Number.isFinite(id) || id <= 0) notFound();

  return (
    <PageShell>
      <OrderForecastPage id={id} />
    </PageShell>
  );
}
