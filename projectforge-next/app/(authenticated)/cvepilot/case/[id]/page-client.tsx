"use client";

import { notFound } from "next/navigation";
import { useRouteParams } from "@/hooks/use-route-params";
import { CaseFilePage } from "@/components/features/cve-pilot/file/case-file-page";

// Reads the id from the URL at runtime (see page.tsx and use-route-params.ts).
export function CvePilotCasePageClient() {
  const raw = useRouteParams<{ id: string }>("/cvepilot/case/[id]")?.id;
  if (raw === undefined) return null;
  const id = Number(raw);
  if (!Number.isFinite(id) || id <= 0) notFound();
  return <CaseFilePage caseId={id} />;
}
