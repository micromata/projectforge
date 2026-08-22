/**
 * The calls of the outgoing invoice (`OutgoingInvoiceEntityRest`) that are neither a list, a read nor a
 * write of the entity: the two exports, the live sums of an unsaved form, and the three reads the form
 * needs for its own defaults.
 *
 * The exports act on the filter the list is showing rather than on a selection, which is why they take
 * one: the backend runs the same query the list ran and exports its whole result set, not the page in
 * view. `recalculate` is here rather than behind `postEntityAction` because it answers a plain sums
 * object instead of a `ResponseAction`.
 */

import { request } from "./client";
import { downloadFile, downloadPost } from "./download";
import { downloadListExcel } from "./list-export";
import type { MagicFilter, PostData } from "./types";

/**
 * The filtered invoices as the Excel file Wicket's "Excel export" produces — one row per invoice.
 *
 * The generic list export of this category, so it goes through [downloadListExcel]: every list whose
 * `*PagesRest` implements `exportAsExcel` answers the same way, and this is the invoice's name for it.
 *
 * A 404 means the filter matched nothing; the caller says so rather than reporting an error (see
 * InvoiceListActions).
 */
export function downloadInvoiceExcel(
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<void> {
  return downloadListExcel("outgoingInvoice", filter, signal);
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

/**
 * The invoice as a Word document, from the template variant given (an empty string for the unnamed one).
 *
 * By id and not from the form: the document is built from the **stored** invoice, whose account and
 * customer the backend can resolve for the address block and the file name — see
 * `OutgoingInvoiceEntityRest.exportInvoiceWord` for why that differs from Wicket, which exports the unsaved
 * form. So the caller offers it for a saved invoice only.
 *
 * A 404 means either no such invoice or no readable template; the caller reports both as an error, since
 * an export the user asked for produced nothing.
 */
export function downloadInvoiceWord(
  id: number,
  variant: string,
  signal?: AbortSignal
): Promise<void> {
  const query = variant ? `?variant=${encodeURIComponent(variant)}` : "";
  return downloadFile(
    `/rs/outgoingInvoice/exportInvoiceWord/${id}${query}`,
    { method: "GET" },
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

/** One entry of the `sellerBankAccount` select — the value is the IBAN, which is what the column holds. */
export interface InvoiceBankAccount {
  value: string;
  label: string;
}

/** What `OutgoingInvoiceEntityRest.getFormDefaults` answers (`FormDefaults` there). */
export interface InvoiceFormDefaults {
  /** `fibu.defaultVAT` as a factor (0.19 for 19 %), null where the installation configured none. */
  defaultVat?: number | null;
  bankAccounts: InvoiceBankAccount[];
  /** Whether the seller address is complete enough for an e-invoice export. */
  eInvoiceConfigured: boolean;
  /** Variants of the Word invoice template; a single empty string means one unnamed variant. */
  templateVariants: string[];
}

/**
 * Everything the form needs before the user touches it, in one read: the default VAT rate, the seller's
 * bank accounts, whether an e-invoice can be exported at all, and the template variants.
 *
 * All four are configuration rather than properties of an invoice, which is why none of them arrives with
 * the entity. Practically immutable, so the caller caches them generously
 * (see `use-invoice-form-defaults.ts`).
 */
export function fetchInvoiceFormDefaults(
  signal?: AbortSignal
): Promise<InvoiceFormDefaults> {
  return request<InvoiceFormDefaults>(
    "/rs/outgoingInvoice/formDefaults",
    { method: "GET" },
    signal
  );
}

/** What `OutgoingInvoiceEntityRest.validateEInvoice` answers (`EInvoiceValidation` there). */
export interface EInvoiceValidation {
  /**
   * Whether `projectforge.einvoice.seller.*` is configured. Nothing the user editing this invoice can fix,
   * but it needs no separate treatment either: an unconfigured seller is the first entry of `errors` below
   * (`fibu.rechnung.eInvoice.error.sellerNotConfigured`), and the checklist names it like any other problem.
   */
  configured: boolean;
  /**
   * What is missing on the invoice, empty where an e-invoice can be built.
   *
   * Sentences, not keys, and already in the user's language: `EInvoiceExportService.validate` translates them
   * because every caller — Wicket's error line as well as this endpoint — puts them in front of a user
   * unchanged.
   */
  errors: string[];
}

/**
 * Query key of the validation below — beside the fetch, like the other query keys of this app
 * (`attachmentsQueryKey`, `historyQueryKey`), so a second reader of the same answer finds it here.
 *
 * Under `["outgoingInvoice", …]` on purpose: every write of the invoice invalidates that prefix
 * (`listQueryKey`), which is what a save from anywhere else on the page has to reach.
 */
export function eInvoiceQueryKey(id: number | null) {
  return ["outgoingInvoice", "eInvoice", id] as const;
}

/**
 * What stands between this invoice and an e-invoice of it — read before the exports are offered.
 *
 * Both exports refuse an invoice that isn't ready, and a refused download says nothing about which field to
 * correct. Of the stored invoice, like the exports themselves: the ZUGFeRD path reads the PDF and the
 * attachments from the JCR by id, so there is no unsaved state it could be asked about.
 */
export function fetchEInvoiceValidation(
  id: number,
  signal?: AbortSignal
): Promise<EInvoiceValidation> {
  return request<EInvoiceValidation>(
    `/rs/outgoingInvoice/eInvoice/${id}/validate`,
    { method: "GET" },
    signal
  );
}

/** The invoice as XRechnung, i.e. the XML alone. */
export function downloadXRechnung(
  id: number,
  signal?: AbortSignal
): Promise<void> {
  return downloadFile(
    `/rs/outgoingInvoice/eInvoice/${id}/xrechnung`,
    { method: "GET" },
    signal
  );
}

/**
 * The invoice as a ZUGFeRD PDF, i.e. a PDF carrying the same XML.
 *
 * The document it is embedded into is the uploaded invoice PDF, or the Word template converted where none
 * was uploaded (see InvoicePdfField).
 */
export function downloadZugferd(
  id: number,
  signal?: AbortSignal
): Promise<void> {
  return downloadFile(
    `/rs/outgoingInvoice/eInvoice/${id}/zugferd`,
    { method: "GET" },
    signal
  );
}

/** A cost unit as `Kost2` travels — `displayName` is what `KostFormatter` composes. */
export interface Kost2Option {
  id?: number | null;
  displayName?: string | null;
}

/**
 * The active cost units of a project, for the Kost2 preselection of a new cost assignment.
 *
 * Asked of the backend rather than derived here: which cost units belong to a project follows from its
 * number range, area and number, and the invoice carries its project without any of the three.
 */
export function fetchActiveKost2(
  projektId: number,
  signal?: AbortSignal
): Promise<Kost2Option[]> {
  return request<Kost2Option[]>(
    `/rs/outgoingInvoice/activeKost2?projektId=${projektId}`,
    { method: "GET" },
    signal
  );
}

/**
 * Whether a cost unit belongs to the project — or, for an invoice naming none, to the customer — of the
 * invoice. False is what the form warns about, as Wicket outlines the field
 * (`RechnungEditForm.onRenderCostRow`).
 */
export function fetchKost2Check(
  kost2Id: number,
  projektId: number | null | undefined,
  kundeId: number | null | undefined,
  signal?: AbortSignal
): Promise<{ matchesInvoice: boolean }> {
  const params = new URLSearchParams({ kost2Id: String(kost2Id) });
  if (projektId != null) params.set("projektId", String(projektId));
  if (kundeId != null) params.set("kundeId", String(kundeId));
  return request<{ matchesInvoice: boolean }>(
    `/rs/outgoingInvoice/kost2Check?${params.toString()}`,
    { method: "GET" },
    signal
  );
}
