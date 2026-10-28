/**
 * The e-invoice checker (`org.projectforge.rest.fibu.EInvoiceCheckerRest`, page `/next/eInvoiceChecker`):
 * upload of a ZUGFeRD PDF or XRechnung XML, the last result of the session and the attachments embedded in it.
 *
 * A file that can't be checked at all (empty, wrong type, too large, unreadable) is an HTTP 400 whose body is
 * the backend's translated text, thrown as an [RsError] carrying exactly that. An e-invoice with errors is a
 * regular result: its errors are part of [EInvoiceData.validationErrors].
 */

import { rawRequest, RsError } from "./client";
import { downloadFile } from "./download";
import { uploadWithProgress, type UploadOptions } from "./upload";

const BASE = "/rs/eInvoiceChecker";

export const E_INVOICE_CHECK_QUERY_KEY = ["eInvoiceChecker"] as const;

/** `EInvoiceParty` — seller or buyer. */
export interface EInvoiceParty {
  name?: string | null;
  street?: string | null;
  zip?: string | null;
  city?: string | null;
  country?: string | null;
  vatId?: string | null;
  email?: string | null;
  contactName?: string | null;
  iban?: string | null;
  bic?: string | null;
}

/** `EInvoiceLineItem`; `vatPercent` is the rate in percent (19, not 0.19). */
export interface EInvoiceLineItem {
  id: number;
  position: number;
  description?: string | null;
  quantity?: number | null;
  unit?: string | null;
  unitPrice?: number | null;
  netAmount?: number | null;
  vatPercent?: number | null;
}

/** `EInvoiceAttachment`; `sizeFormatted` arrives formatted by the backend. */
export interface EInvoiceAttachment {
  id: number;
  filename: string;
  mimeType?: string | null;
  description?: string | null;
  size: number;
  sizeFormatted?: string | null;
}

/**
 * `EInvoiceData`. The dates are `yyyy-MM-dd` where the parser could read them as dates, otherwise the raw
 * text of the file.
 */
export interface EInvoiceData {
  invoiceNumber?: string | null;
  issueDate?: string | null;
  dueDate?: string | null;
  currency?: string | null;
  documentTypeCode?: string | null;
  buyerReference?: string | null;
  seller?: EInvoiceParty | null;
  buyer?: EInvoiceParty | null;
  totalNetAmount?: number | null;
  totalGrossAmount?: number | null;
  totalTaxAmount?: number | null;
  amountDue?: number | null;
  lineItems: EInvoiceLineItem[];
  attachments: EInvoiceAttachment[];
  validationErrors: string[];
  warnings: string[];
  format?: string | null;
  profile?: string | null;
}

/** `EInvoiceCheckerRest.CheckResult`. */
export interface EInvoiceCheckResult {
  filename: string;
  invoiceData: EInvoiceData;
}

/** The result of the last upload of this session, null if there is none (or it expired). */
export async function fetchLastCheck(
  signal?: AbortSignal
): Promise<EInvoiceCheckResult | null> {
  const path = `${BASE}/result`;
  const res = await rawRequest(path, { method: "GET" }, signal);
  if (res.status === 204) return null;
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  return (await res.json()) as EInvoiceCheckResult;
}

/** Checks the given file; the result replaces the session's last one. */
export async function uploadEInvoice(
  file: File,
  options: UploadOptions = {}
): Promise<EInvoiceCheckResult> {
  const body = new FormData();
  body.append("file", file);
  const path = `${BASE}/upload`;
  const res = await uploadWithProgress(path, body, options);
  if (res.status < 200 || res.status >= 300) {
    throw new RsError(res.status, res.text || `${res.status}: ${path}`);
  }
  return JSON.parse(res.text) as EInvoiceCheckResult;
}

/** Saves an attachment embedded in the last checked file. */
export function downloadEInvoiceAttachment(id: number): Promise<void> {
  return downloadFile(`${BASE}/attachment/${id}`);
}
