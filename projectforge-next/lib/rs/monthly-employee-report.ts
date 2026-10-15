/**
 * The monthly employee report ("Monatsbericht", `org.projectforge.rest.MonthlyEmployeeReportRest`): the
 * successor of Wicket's `wa/monthlyEmployeeReport`. A non-entity, standalone report, so it has its own
 * small client here rather than going through `fetchList` / the entity plumbing.
 */

import { request } from "./client";
import { downloadPost } from "./download";
import type {
  MonthlyReport,
  MonthlyReportQuery,
} from "@/components/features/monthly-employee-report/types";
import type { InvoicingQuotaHistory } from "@/components/shared/invoicing-quota/types";

function toParams(query: MonthlyReportQuery): string {
  const params = new URLSearchParams();
  if (query.userId != null) params.set("userId", String(query.userId));
  if (query.year != null) params.set("year", String(query.year));
  if (query.month != null) params.set("month", String(query.month));
  if (query.showInvoicingQuota != null)
    params.set("showInvoicingQuota", String(query.showInvoicingQuota));
  const s = params.toString();
  return s ? `?${s}` : "";
}

/** The report for the given user/year/month; omitted parts default to the logged-in user / current month. */
export function fetchMonthlyEmployeeReport(
  query: MonthlyReportQuery,
  signal?: AbortSignal
): Promise<MonthlyReport> {
  return request<MonthlyReport>(
    `/rs/monthlyEmployeeReport${toParams(query)}`,
    { method: "GET" },
    signal
  );
}

/**
 * The invoicing quota of the 12 months ending with the query's month (the chart tab). 403 unless the logged-in
 * user may see the quota of that user.
 */
export function fetchInvoicingQuotaHistory(
  query: MonthlyReportQuery,
  signal?: AbortSignal
): Promise<InvoicingQuotaHistory> {
  return request<InvoicingQuotaHistory>(
    `/rs/monthlyEmployeeReport/invoicingQuotaHistory${toParams({
      userId: query.userId,
      year: query.year,
      month: query.month,
    })}`,
    { method: "GET" },
    signal
  );
}

/** The years that have time sheets for the user (defaults to the logged-in user), newest first. */
export function fetchMonthlyReportYears(
  userId: number | undefined,
  signal?: AbortSignal
): Promise<number[]> {
  const params = userId != null ? `?userId=${userId}` : "";
  return request<number[]>(
    `/rs/monthlyEmployeeReport/years${params}`,
    { method: "GET" },
    signal
  );
}

/** Downloads the report as PDF (reuses the legacy Apache FOP stylesheet); the filename comes from the server. */
export function downloadMonthlyEmployeeReportPdf(
  query: MonthlyReportQuery,
  signal?: AbortSignal
): Promise<void> {
  return downloadPost("/rs/monthlyEmployeeReport/exportPdf", query, signal);
}
