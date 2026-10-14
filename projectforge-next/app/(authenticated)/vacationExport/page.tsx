"use client";

import { Suspense } from "react";
import { VacationExportPage } from "@/components/features/vacation/vacation-export-page";

/**
 * The team vacation calendar (`/next/vacationExport`), the successor of the React page
 * `react/vacationExport/dynamic`. A non-entity, standalone page like the vacation account.
 *
 * The `<Suspense>` boundary is required because the page reads the period (`?period=&from=`) via
 * `useSearchParams` under the static export (`output: "export"`).
 */
export default function VacationExportRoute() {
  return (
    <Suspense>
      <VacationExportPage />
    </Suspense>
  );
}
