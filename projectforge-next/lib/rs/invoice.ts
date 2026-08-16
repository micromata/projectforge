/**
 * The two exports of the outgoing invoice list (`OutgoingInvoiceEntityRest`).
 *
 * Both act on the filter the list is showing rather than on a selection, which is why they take one: the
 * backend runs the same query the list ran and exports its whole result set, not the page in view.
 */

import { downloadPost } from "./download";
import type { MagicFilter } from "./types";

/**
 * The filtered invoices as the Excel file Wicket's "Excel export" produces — one row per invoice.
 *
 * A 404 means the filter matched nothing; the caller says so rather than reporting an error (see
 * InvoiceListActions).
 */
export function downloadInvoiceExcel(
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<void> {
  return downloadPost("/rs/outgoingInvoice/exportAsExcel", filter, signal);
}

/**
 * The same invoices with one row per cost assignment (`KostZuweisungExport`).
 *
 * Answers 404 where no cost ids are configured, exactly as the Wicket menu entry is absent there — so the
 * caller offers this export unconditionally and reports the empty answer, instead of asking the backend
 * beforehand whether the installation uses cost assignments at all.
 */
export function downloadInvoiceCostAssignmentsExcel(
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<void> {
  return downloadPost(
    "/rs/outgoingInvoice/exportCostAssignmentsAsExcel",
    filter,
    signal
  );
}
