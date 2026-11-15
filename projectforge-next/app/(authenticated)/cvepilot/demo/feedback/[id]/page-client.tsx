"use client";

import { useRouteParams } from "@/hooks/use-route-params";
import { CampaignPage } from "@/components/features/cve-pilot/feedback/campaign-page";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function CvePilotCampaignPageClient() {
  const id = useRouteParams<{ id: string }>("/cvepilot/demo/feedback/[id]")?.id;
  if (id === undefined) return null;
  return <CampaignPage id={decodeURIComponent(id)} />;
}
