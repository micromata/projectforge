"use client";

import { useRouteParams } from "@/hooks/use-route-params";
import { SystemPage } from "@/components/features/cve-pilot/system/system-page";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function CvePilotSystemPageClient() {
  const id = useRouteParams<{ id: string }>("/cvepilot/demo/system/[id]")?.id;
  if (id === undefined) return null;
  return <SystemPage id={decodeURIComponent(id)} />;
}
