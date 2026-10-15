"use client";

import { Suspense } from "react";
import { MonthlyEmployeeReportPage } from "@/components/features/monthly-employee-report/monthly-employee-report-page";

/**
 * The monthly employee report (`/next/monthlyEmployeeReport`), the successor of Wicket's
 * `wa/monthlyEmployeeReport`. A non-entity, standalone page like the global search.
 *
 * The `<Suspense>` boundary is required because the page reads the optional deep-link `?userId=&year=&month=`
 * via `useSearchParams` under the static export (`output: "export"`), same as the search page.
 */
export default function MonthlyEmployeeReportRoute() {
  return (
    <Suspense>
      <MonthlyEmployeeReportPage />
    </Suspense>
  );
}
