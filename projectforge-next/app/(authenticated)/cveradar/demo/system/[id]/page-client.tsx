"use client";

import { useRouteParams } from "@/hooks/use-route-params";
import { SystemPage } from "@/components/features/cve-radar/system/system-page";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function CveRadarSystemPageClient() {
  const id = useRouteParams<{ id: string }>("/cveradar/demo/system/[id]")?.id;
  if (id === undefined) return null;
  return <SystemPage id={decodeURIComponent(id)} />;
}
