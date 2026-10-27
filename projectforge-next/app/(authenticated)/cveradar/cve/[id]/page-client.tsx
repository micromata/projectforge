"use client";

import { useRouteParams } from "@/hooks/use-route-params";
import { VulnerabilityPage } from "@/components/features/cve-radar/vulnerability/vulnerability-page";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function CveRadarVulnerabilityPageClient() {
  const id = useRouteParams<{ id: string }>("/cveradar/cve/[id]")?.id;
  if (id === undefined) return null;
  return <VulnerabilityPage id={decodeURIComponent(id)} />;
}
