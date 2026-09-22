/**
 * The invoice PDF of one outgoing invoice — the single file the ZUGFeRD export embeds its XML into
 * (`OutgoingInvoiceEntityRest.getInvoicePdfInfo` and friends).
 *
 * Not an attachment list although it is stored as an attachment: there is exactly one per invoice, marked
 * by its description (`EInvoiceExportService.INVOICE_PDF_MARKER`), and uploading a second one replaces the
 * first. So the three calls here are read/replace/remove of one value, not the add/rename/delete of a list
 * (see ./attachments.ts for that).
 */

import { request, RsError } from "./client";
import { uploadWithProgress, type UploadOptions } from "./upload";

/**
 * What the form shows of the stored PDF; `sizeHumanReadable` arrives formatted by the backend.
 *
 * `fileId` is what the download is built from: the file lies in the invoice's regular attachment node, so
 * `attachmentDownloadUrl` (./attachments.ts) serves it — there is no download endpoint under this BASE.
 */
export interface InvoicePdfInfo {
  name?: string | null;
  sizeHumanReadable?: string | null;
  fileId?: string | null;
}

/** `OutgoingInvoiceEntityRest.InvoicePdfState` — `pdf` is null where the invoice has none. */
export interface InvoicePdfState {
  pdf?: InvoicePdfInfo | null;
}

const BASE = "/rs/outgoingInvoice/invoicePdf";

/**
 * The cache entry of one invoice's PDF.
 *
 * Exported because a second place writes to it: the attachment list of the same page shows this very file
 * and can delete it there, after which this entry describes a file that is gone (see AttachmentSection).
 */
export function invoicePdfQueryKey(invoiceId: number | null) {
  return ["outgoingInvoice", "invoicePdf", invoiceId] as const;
}

export function fetchInvoicePdfInfo(
  invoiceId: number,
  signal?: AbortSignal
): Promise<InvoicePdfState> {
  return request<InvoicePdfState>(
    `${BASE}/${invoiceId}/info`,
    { method: "GET" },
    signal
  );
}

/**
 * Stores a PDF as *the* invoice PDF, replacing whatever was there, and answers the new state.
 *
 * Through `uploadWithProgress` like every other upload of this app: a scanned invoice is megabytes, and
 * `fetch` cannot report how much of it has gone out (see ./upload.ts).
 *
 * A refusal — not a PDF, or too large — is an HTTP 400 whose body is the backend's own translated text
 * (`FileCheck`), so it is thrown as an [RsError] carrying exactly that: our own wording would be a second,
 * less precise answer to a question the backend already answered.
 */
export async function uploadInvoicePdf(
  invoiceId: number,
  file: File,
  options: UploadOptions = {}
): Promise<InvoicePdfState> {
  const body = new FormData();
  body.append("file", file);
  const path = `${BASE}/${invoiceId}`;
  const res = await uploadWithProgress(path, body, options);
  if (res.status < 200 || res.status >= 300) {
    throw new RsError(res.status, res.text || `${res.status}: ${path}`);
  }
  return JSON.parse(res.text) as InvoicePdfState;
}

/** Removes the invoice PDF, so the export converts the Word template again. Answers the empty state. */
export function deleteInvoicePdf(
  invoiceId: number,
  signal?: AbortSignal
): Promise<InvoicePdfState> {
  return request<InvoicePdfState>(
    `${BASE}/${invoiceId}`,
    { method: "DELETE" },
    signal
  );
}
