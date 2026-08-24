"use client";

import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { AttachmentList } from "@/components/shared/attachments/attachment-list";
import { leafKeyOf } from "@/lib/leaf-key";
import { invoicePdfQueryKey } from "@/lib/rs/invoice-pdf";

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
 * The invoice PDF is among them ([INVOICE_PDF_MARKER]): it is one of the files of this invoice, so it is
 * downloaded and deleted here like any other. Only renaming it is withheld, because its description is the
 * marker that makes it the invoice PDF — which is also why the row shows the name of the field it belongs
 * to (InvoicePdfField in EInvoiceSection) instead of that marker.
 *
 * @param invoiceId null for an invoice being added — nothing can be attached before the first save, since
 * the JCR node hangs off the persisted id.
 */
export function AttachmentSection({ invoiceId }: { invoiceId: number | null }) {
  const t = useTranslations();
  const qc = useQueryClient();
  return (
    // embedded: inline in the form, so the compact toolbar instead of a permanent drop box.
    <AttachmentList
      entity="outgoingInvoice"
      id={invoiceId}
      embedded
      lockedDescription={INVOICE_PDF_MARKER}
      // `_`, since the key has a `hint` subkey and so becomes a namespace.
      lockedLabel={t(leafKeyOf("fibu.rechnung.invoicePdf", t.has))}
      // The invoice PDF can be deleted here, and then the field above it still holds the file in its own
      // query — so that one is dropped whenever these files changed (see InvoicePdfField).
      onChanged={() =>
        void qc.invalidateQueries({ queryKey: invoicePdfQueryKey(invoiceId) })
      }
    />
  );
}
