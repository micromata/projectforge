"use client";

import { useRouteParams } from "@/hooks/use-route-params";
import { VulnerabilityPage } from "@/components/features/cve-clinic/vulnerability/vulnerability-page";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function CveClinicVulnerabilityPageClient() {
  const id = useRouteParams<{ id: string }>("/cveclinic/demo/cve/[id]")?.id;
  if (id === undefined) return null;
  return <VulnerabilityPage id={decodeURIComponent(id)} />;
}
