"use client";

import { ReportObjectivesPage } from "@/components/features/report-objectives/report-objectives-page";

/**
 * The report objectives route (`/next/reportObjectives`): upload of a ReportObjective XML and the BWA
 * drill-down of its reports. Takes no parameters, so no `<Suspense>`/`useSearchParams` is needed.
 */
export default function ReportObjectivesRoute() {
  return <ReportObjectivesPage />;
}
