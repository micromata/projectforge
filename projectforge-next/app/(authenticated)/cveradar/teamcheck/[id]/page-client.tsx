"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { TeamCheckFilePage } from "@/components/features/cve-radar/file/team-check-file-page";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function CveRadarTeamcheckPageClient() {
  const raw = useRouteParams<{ id: string }>("/cveradar/teamcheck/[id]")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  if (!Number.isFinite(id) || id <= 0) notFound();
  return <TeamCheckFilePage id={id} />;
}
