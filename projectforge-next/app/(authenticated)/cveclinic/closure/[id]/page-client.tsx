"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { ClosurePage } from "@/components/features/cve-clinic/live/closure-page";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function CveClinicClosurePageClient() {
  const raw = useRouteParams<{ id: string }>("/cveclinic/closure/[id]")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  if (!Number.isFinite(id) || id <= 0) notFound();
  return <ClosurePage id={id} />;
}
