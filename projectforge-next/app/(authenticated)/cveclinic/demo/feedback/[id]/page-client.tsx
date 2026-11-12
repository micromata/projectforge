"use client";

import { useRouteParams } from "@/hooks/use-route-params";
import { CampaignPage } from "@/components/features/cve-clinic/feedback/campaign-page";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function CveClinicCampaignPageClient() {
  const id = useRouteParams<{ id: string }>("/cveclinic/demo/feedback/[id]")?.id;
  if (id === undefined) return null;
  return <CampaignPage id={decodeURIComponent(id)} />;
}
