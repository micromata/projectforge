"use client";

import { AttachmentList } from "@/components/shared/attachments/attachment-list";

/**
 * The attachments of an invoice — the `UIAttachmentList` the legacy edit layout is reduced to
 * (OutgoingInvoiceEntityRest, whose title `attachment.list` the section declares).
 *
 * Nothing but the entity name: attachments are not an invoice feature, every `AbstractPagesRest` entity
 * can have them (see components/shared/attachments/).
 *
 * Not the same thing as the invoice PDF Wicket uploads on this page (`fibu.rechnung.invoicePdf`), which
 * feeds the e-invoice export and is out of scope here.
 *
 * @param invoiceId null for an invoice being added — nothing can be attached before the first save, since
 * the JCR node hangs off the persisted id.
 */
export function AttachmentSection({ invoiceId }: { invoiceId: number | null }) {
  // embedded: inline in the form, so the compact toolbar instead of a permanent drop box.
  return <AttachmentList entity="outgoingInvoice" id={invoiceId} embedded />;
}
