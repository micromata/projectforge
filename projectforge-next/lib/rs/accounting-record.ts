/**
 * The report drill-down of the accounting-record list (`AccountingRecordEntityRest.getReportRecords`).
 *
 * A standalone read that does not go through the entity list plumbing: instead of a DB query it returns a
 * fixed set of records taken from the per-user, in-memory `ReportStorage` the (still-Wicket) Reporting UI
 * built. Entered from that UI's links as `/next/accounting-record?reportId=…[&businessAssessmentRowId=…]`.
 * If the storage is absent (never opened, or a JVM restart dropped it) the result is empty — the same
 * limitation the Wicket list has.
 */

import { request } from "./client";
import type { ReportRecordsResult } from "@/components/features/accounting-record/types";

/**
 * The records of one report — the whole report, or the subset behind one of its BWA rows.
 *
 * @param reportId The report's id in the per-user `ReportStorage`.
 * @param businessAssessmentRowId The BWA row to drill into; omitted for the report's full record set.
 */
export function fetchReportRecords(
  reportId: string,
  businessAssessmentRowId?: string | null,
  signal?: AbortSignal
): Promise<ReportRecordsResult> {
  const params = new URLSearchParams({ reportId });
  if (businessAssessmentRowId != null && businessAssessmentRowId !== "") {
    params.set("businessAssessmentRowId", businessAssessmentRowId);
  }
  return request<ReportRecordsResult>(
    `/rs/accountingRecord/reportRecords?${params.toString()}`,
    { method: "GET" },
    signal
  );
}
