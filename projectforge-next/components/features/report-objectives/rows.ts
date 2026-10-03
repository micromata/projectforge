import type { ReportRow, ReportRowPriority } from "./types";

const PRIORITY_ORDER: ReportRowPriority[] = [
  "LEAST",
  "LOW",
  "MIDDLE",
  "HIGH",
  "HIGHEST",
];

/**
 * The rows shown: all of them, or only the important ones (priority HIGH and above), as the Wicket
 * page showed them. A row without priority counts as MIDDLE, the backend's default.
 */
export function visibleRows(rows: ReportRow[], showAll: boolean): ReportRow[] {
  if (showAll) return rows;
  const min = PRIORITY_ORDER.indexOf("HIGH");
  return rows.filter(
    (row) => PRIORITY_ORDER.indexOf(row.priority ?? "MIDDLE") >= min
  );
}

/** The drill-down into the accounting records of a report, or of one BWA row of it. */
export function recordsHref(reportId: string, rowNo?: string | null): string {
  const params = new URLSearchParams({ reportId });
  if (rowNo) params.set("businessAssessmentRowId", rowNo);
  // Without "/next": the router prepends the basePath itself.
  return `/accounting-record?${params.toString()}`;
}
