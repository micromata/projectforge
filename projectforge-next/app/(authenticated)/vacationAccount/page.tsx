"use client";

import { Suspense } from "react";
import { VacationAccountPage } from "@/components/features/vacation-account/vacation-account-page";

/**
 * The vacation account (`/next/vacationAccount`), the successor of the React page `react/vacationAccount/dynamic`.
 * A non-entity, standalone page like the HR view.
 *
 * The `<Suspense>` boundary is required because the page reads the optional `?employeeId=` (HR staff) via
 * `useSearchParams` under the static export (`output: "export"`).
 */
export default function VacationAccountRoute() {
  return (
    <Suspense>
      <VacationAccountPage />
    </Suspense>
  );
}
