/**
 * The calls of the outgoing invoice (`OutgoingInvoiceEntityRest`) that are neither a list, a read nor a
 * write of the entity: the two exports, and the live sums of an unsaved form.
 *
 * The exports act on the filter the list is showing rather than on a selection, which is why they take
 * one: the backend runs the same query the list ran and exports its whole result set, not the page in
 * view. `recalculate` is here rather than behind `postEntityAction` because it answers a plain sums
 * object instead of a `ResponseAction`.
 */

import { request } from "./client";
import { downloadPost } from "./download";
import type { MagicFilter, PostData } from "./types";

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

/** Sums of one position, matched by its number — a new position has no id yet. */
export interface InvoicePositionSums {
  number?: number | null;
  netSum?: number | null;
  vatAmount?: number | null;
  grossSum?: number | null;
  /** Net sum of this position's cost assignments. */
  kostZuweisungNetSum?: number | null;
  /**
   * How much of the position's net sum is not assigned to a cost unit yet — **negated**, as
   * `RechnungPosInfo` computes it: an unassigned rest of 400,00 € reads as -400,00. A hint only, since
   * `RechnungDao` validates no cost assignment sums.
   */
  kostZuweisungNetFehlbetrag?: number | null;
}

/** What `OutgoingInvoiceEntityRest.recalculate` answers (`InvoiceSums` there). */
export interface InvoiceSums {
  netSum?: number | null;
  vatAmount?: number | null;
  grossSum?: number | null;
  /** Gross sum minus a discount that was taken — the amount the invoice actually comes to. */
  grossSumWithDiscount?: number | null;
  kostZuweisungenNetSum?: number | null;
  /** The same difference as above for the whole invoice, but **not** negated (`RechnungInfo`). */
  kostZuweisungenFehlbetrag?: number | null;
  bezahlt?: boolean | null;
  ueberfaellig?: boolean | null;
  positions?: InvoicePositionSums[] | null;
}

/**
 * Recalculates every sum of an invoice from the **unsaved** form state.
 *
 * Needed rather than convenient: how a position is rounded before it enters a sum is German law and
 * `RechnungCalculator`'s rule (`roundPositionsBeforeSum`), and the caches only know saved invoices. So
 * the backend builds a transient `RechnungDO` from the posted DTO and computes on that, with
 * `useCaches = false` — the posted positions have no ids to look anything up by.
 *
 * Deleted rows may be sent along untouched: the calculator skips them itself.
 *
 * @param data The form's values, i.e. the same `Rechnung` DTO a save would send.
 */
export function recalculateInvoice(
  data: unknown,
  signal?: AbortSignal
): Promise<InvoiceSums> {
  const postData: PostData = { data } as PostData;
  return request<InvoiceSums>(
    "/rs/outgoingInvoice/recalculate",
    { method: "POST", body: JSON.stringify(postData) },
    signal
  );
}
