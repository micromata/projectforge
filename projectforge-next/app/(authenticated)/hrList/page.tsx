"use client";

import { Suspense } from "react";
import { HrViewPage } from "@/components/features/hr-view/hr-view-page";

/**
 * The HR view (`/next/hrList`), the successor of Wicket's `wa/hrList`. A non-entity, standalone page like the
 * monthly employee report.
 *
 * The `<Suspense>` boundary is required because the page reads the optional deep-link `?startDay=&stopDay=`
 * via `useSearchParams` under the static export (`output: "export"`).
 */
export default function HrListRoute() {
  return (
    <Suspense>
      <HrViewPage />
    </Suspense>
  );
}
