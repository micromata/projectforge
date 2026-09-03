/**
 * The calls of the incoming invoice (`IncomingInvoiceEntityRest`) that are neither a list, a read nor a
 * write of the entity: the two exports, the live sums of an unsaved form, and the one read the form needs
 * for its own default.
 *
 * Far fewer than the outgoing invoice's (see lib/rs/invoice.ts): a creditor invoice has no e-invoice, no
 * Word template and no bank accounts of its own, so `formDefaults` carries only the VAT rate and there is
 * no e-invoice endpoint here at all. The recalculation and the two Excel exports are the same shape as the
 * outgoing invoice's, under this category's path.
 */

import { request } from "./client";
import { downloadFile, downloadPost } from "./download";
import { recalculateInvoiceSums, type InvoiceSums } from "./invoice-sums";
import { downloadListExcel } from "./list-export";
import type { MagicFilter } from "./types";

/** REST category of the incoming invoice — `IncomingInvoiceEntityRest` is mapped to "incomingInvoice". */
const ENTITY = "incomingInvoice";

/**
 * The filtered invoices as the Excel file Wicket's "Excel export" produces — one row per invoice.
 *
 * The generic list export of this category, so it goes through [downloadListExcel]. A 404 means the filter
 * matched nothing; the caller says so rather than reporting an error (see CreditorInvoiceListActions).
 */
export function downloadCreditorInvoiceExcel(
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<void> {
  return downloadListExcel(ENTITY, filter, signal);
}

/**
 * The same invoices with one row per cost assignment (`KostZuweisungExport`).
 *
 * Answers 404 where no cost ids are configured, exactly as the Wicket menu entry is absent there — so the
 * caller offers this export unconditionally and reports the empty answer, instead of asking the backend
 * beforehand whether the installation uses cost assignments at all.
 */
export function downloadCreditorInvoiceCostAssignmentsExcel(
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<void> {
  return downloadPost(
    `/rs/${ENTITY}/exportCostAssignmentsAsExcel`,
    filter,
    signal
  );
}

/**
 * The SEPA bank transfer of one stored invoice as a pain.001.003.03 xml (`exportTransfer/{id}`), the
 * single-invoice counterpart of Wicket's `EingangsrechnungEditPage` "Export bank transfers" button.
 *
 * A GET, so the invoice's registered session selection is irrelevant — it exports the invoice named by [id].
 * The document is built from the **stored** invoice, so the caller offers it for a saved invoice only. A
 * validation failure (missing IBAN/BIC/…) arrives as a downloadable `error.txt`, exactly as Wicket's does.
 */
export function downloadCreditorInvoiceTransfer(
  id: number,
  signal?: AbortSignal
): Promise<void> {
  return downloadFile(
    `/rs/${ENTITY}/exportTransfer/${id}`,
    { method: "GET" },
    signal
  );
}

/**
 * The SEPA bank transfer of the mass-selected invoices as one pain.001.003.03 xml
 * (`incomingInvoiceSelected/exportTransfers`).
 *
 * The selected ids are not passed: they live in the HTTP session, registered there by the selection mode
 * (see multi-select.ts). A validation failure — no selection, a foreign-currency invoice, or missing fields —
 * arrives as a downloadable `error.txt`.
 */
export function downloadCreditorInvoiceTransfers(
  signal?: AbortSignal
): Promise<void> {
  return downloadFile(
    "/rs/incomingInvoiceSelected/exportTransfers",
    { method: "GET" },
    signal
  );
}

/**
 * Recalculates every sum of an incoming invoice from the **unsaved** form state — the shared call fixed to
 * this category (see recalculateInvoiceSums for the why).
 *
 * @param data The form's values, i.e. the same `Eingangsrechnung` DTO a save would send.
 */
export function recalculateCreditorInvoice(
  data: unknown,
  signal?: AbortSignal
): Promise<InvoiceSums> {
  return recalculateInvoiceSums(ENTITY, data, signal);
}

/** What `IncomingInvoiceEntityRest.getFormDefaults` answers (`FormDefaults` there). */
export interface CreditorInvoiceFormDefaults {
  /** `fibu.defaultVAT` as a factor (0.19 for 19 %), null where the installation configured none. */
  defaultVat?: number | null;
}

/**
 * The one configuration value the form needs before the user touches it: the default VAT rate of a new
 * position.
 *
 * Configuration rather than a property of an invoice, which is why it does not arrive with the entity.
 * Practically immutable, so the caller caches it generously (see `use-creditor-invoice-form-defaults.ts`).
 */
export function fetchCreditorInvoiceFormDefaults(
  signal?: AbortSignal
): Promise<CreditorInvoiceFormDefaults> {
  return request<CreditorInvoiceFormDefaults>(
    `/rs/${ENTITY}/formDefaults`,
    { method: "GET" },
    signal
  );
}
