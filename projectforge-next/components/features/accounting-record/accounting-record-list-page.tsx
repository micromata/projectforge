"use client";

import { useSearchParams } from "next/navigation";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { ACCOUNTING_RECORD_PAGE } from "./accounting-record.page";
import { ReportRecordsView } from "./report-records-view";

/**
 * The accounting-record list route, in one of its two modes.
 *
 * Normal: the entity list with its booking-period filter and BWA (see ACCOUNTING_RECORD_PAGE). Report
 * drill-down: entered from the (still-Wicket) Reporting UI as `?reportId=…[&businessAssessmentRowId=…]`,
 * a fixed record set from the per-user `ReportStorage` with the filter hidden (see ReportRecordsView).
 *
 * The mode is read from the URL, so a bookmarked report link keeps working and the normal list path is
 * left entirely untouched.
 */
export function AccountingRecordListRoute() {
  const params = useSearchParams();
  const reportId = params.get("reportId");
  if (reportId) {
    return (
      <ReportRecordsView
        reportId={reportId}
        businessAssessmentRowId={params.get("businessAssessmentRowId")}
      />
    );
  }
  return <EntityListPage page={ACCOUNTING_RECORD_PAGE} />;
}
