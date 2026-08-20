"use client";

import { AttachmentList } from "@/components/shared/attachments/attachment-list";

/**
 * `EInvoiceExportService.INVOICE_PDF_MARKER` — the description that marks an attachment as *the* invoice
 * PDF of its invoice. A backend convention, spelled out once here so the string appears in a single place
 * (see InvoicePdfField for what the file is for).
 */
export const INVOICE_PDF_MARKER = "__INVOICE_PDF__";

/**
 * The attachments of an invoice — which are also what gets embedded into a ZUGFeRD document
 * (`EInvoiceExportService.embedAttachmentsInPdf`).
 *
 * Nothing but the entity name: attachments are not an invoice feature, every `AbstractPagesRest` entity can
 * have them (see components/shared/attachments/).
 *
 * The invoice PDF is filtered out by [INVOICE_PDF_MARKER], because it is the same storage but not one file
 * among these: it belongs to the e-invoice and is offered there (InvoicePdfField in EInvoiceSection). So no
 * file appears twice on this page.
 *
 * @param invoiceId null for an invoice being added — nothing can be attached before the first save, since
 * the JCR node hangs off the persisted id.
 */
export function AttachmentSection({ invoiceId }: { invoiceId: number | null }) {
  return (
    // embedded: inline in the form, so the compact toolbar instead of a permanent drop box.
    <AttachmentList
      entity="outgoingInvoice"
      id={invoiceId}
      embedded
      excludeDescription={INVOICE_PDF_MARKER}
    />
  );
}
