"use client";

import { AttachmentList } from "@/components/shared/attachments/attachment-list";
import { InvoicePdfField } from "./invoice-pdf-field";

/**
 * `EInvoiceExportService.INVOICE_PDF_MARKER` — the description that marks an attachment as *the* invoice
 * PDF of its invoice. A backend convention, spelled out once here so the string appears in a single place
 * (see InvoicePdfField for what the file is for).
 */
export const INVOICE_PDF_MARKER = "__INVOICE_PDF__";

/**
 * The files of an invoice: the invoice PDF the e-invoice export builds on, and the attachments — which
 * are what gets embedded into a ZUGFeRD document (`EInvoiceExportService.embedAttachmentsInPdf`).
 *
 * Two things on one section because it is one question, "which files belong to this invoice", and because
 * they are the same storage: the invoice PDF *is* an attachment, marked by [INVOICE_PDF_MARKER] and hidden
 * from the list below so no file is offered twice.
 *
 * The attachment part is nothing but the entity name: attachments are not an invoice feature, every
 * `AbstractPagesRest` entity can have them (see components/shared/attachments/).
 *
 * @param invoiceId null for an invoice being added — nothing can be attached before the first save, since
 * the JCR node hangs off the persisted id.
 */
export function AttachmentSection({ invoiceId }: { invoiceId: number | null }) {
  return (
    <div className="flex flex-col gap-4">
      <InvoicePdfField invoiceId={invoiceId} />
      {/* embedded: inline in the form, so the compact toolbar instead of a permanent drop box. */}
      <AttachmentList
        entity="outgoingInvoice"
        id={invoiceId}
        embedded
        excludeDescription={INVOICE_PDF_MARKER}
      />
    </div>
  );
}
