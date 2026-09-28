"use client";

import { Suspense } from "react";
import { AccountingRecordListRoute } from "@/components/features/accounting-record/accounting-record-list-page";

// The `<Suspense>` boundary is required because the route reads `?reportId=&businessAssessmentRowId=`
// via `useSearchParams` under the static export (`output: "export"`), same as the search page.
export default function AccountingRecordPage() {
  return (
    <Suspense>
      <AccountingRecordListRoute />
    </Suspense>
  );
}
