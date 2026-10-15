"use client";

import { PersonalStatisticsPage } from "@/components/features/personal-statistics/personal-statistics-page";

/**
 * The personal statistics page (`/next/personalStatistics`), successor of Wicket's `wa/personalStatistics`.
 * A non-entity, standalone read-only page; it takes no parameters, so no `<Suspense>`/`useSearchParams` is
 * needed (unlike the search and monthly-report pages).
 */
export default function PersonalStatisticsRoute() {
  return <PersonalStatisticsPage />;
}
