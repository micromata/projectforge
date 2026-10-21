"use client";

import { Suspense } from "react";
import { JobsMonitorPage } from "@/components/features/jobs-monitor/jobs-monitor-page";

/**
 * The job monitor (`/next/jobsMonitor`), the successor of the dynamic React page `react/jobsMonitor/dynamic`.
 *
 * The `<Suspense>` boundary is required because the page reads the optional `?jobId=` and `?caller=` (set by
 * the import pages) via `useSearchParams` under the static export (`output: "export"`).
 */
export default function JobsMonitorRoute() {
  return (
    <Suspense>
      <JobsMonitorPage />
    </Suspense>
  );
}
