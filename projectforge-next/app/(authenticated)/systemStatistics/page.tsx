"use client";

import { SystemStatisticsPage } from "@/components/features/system-statistics/system-statistics-page";

/**
 * The system statistics route (`/next/systemStatistics`), successor of the dynamic React page
 * `react/systemStatistics/dynamic`. It takes no parameters, so no `<Suspense>`/`useSearchParams` is needed.
 */
export default function SystemStatisticsRoute() {
  return <SystemStatisticsPage />;
}
