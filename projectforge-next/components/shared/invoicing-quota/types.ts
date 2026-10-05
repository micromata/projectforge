/** Contract of `MonthlyEmployeeReportRest.getInvoicingQuotaHistory` (shared by the monthly report and the personal statistics). */

/** The invoicing quota of one month, for the chart tab (`InvoicingQuotaHistoryMonth`). */
export interface InvoicingQuotaHistoryMonth {
  /** `yyyy-MM`. */
  month: string;
  /** Fraction 0..1, or null for a month without (non-ignored) work time — a gap in the line. */
  quota: number | null;
  billedHours: string | null;
  totalHours: string | null;
}

/** The invoicing quota of the 12 months ending with the selected report month, oldest first. */
export interface InvoicingQuotaHistory {
  months: InvoicingQuotaHistoryMonth[];
}
