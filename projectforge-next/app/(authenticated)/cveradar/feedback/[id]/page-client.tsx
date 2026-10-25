"use client";

import { useRouteParams } from "@/hooks/use-route-params";
import { CampaignPage } from "@/components/features/cve-radar/feedback/campaign-page";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function CveRadarCampaignPageClient() {
  const id = useRouteParams<{ id: string }>("/cveradar/feedback/[id]")?.id;
  if (id === undefined) return null;
  return <CampaignPage id={decodeURIComponent(id)} />;
}
