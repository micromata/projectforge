"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { CaseFilePage } from "@/components/features/cve-radar/file/case-file-page";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function CveRadarCasePageClient() {
  const raw = useRouteParams<{ id: string }>("/cveradar/case/[id]")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  if (!Number.isFinite(id) || id <= 0) notFound();
  return <CaseFilePage caseId={id} />;
}
