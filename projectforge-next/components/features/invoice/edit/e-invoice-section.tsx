"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { eInvoiceQueryKey, fetchEInvoiceValidation } from "@/lib/rs/invoice";
import { EInvoiceActions } from "./e-invoice-actions";
import { EInvoiceChecklist } from "./e-invoice-checklist";
import { InvoicePdfField } from "./invoice-pdf-field";

/**
 * Below the address fields of the `customer` section: the invoice PDF the e-invoice is built into, what is
 * still missing, and the two buttons that save and export — Wicket's `EInvoiceModalDialog`
 * (`RechnungEditForm`), unfolded into the form.
 *
 * A section and not a dialog, because everything in it is *about the fields above it*: the address, the
 * Leitweg-ID and the bank account are what the checklist names, and they are fields of this form. A dialog
 * repeating them would be a second place to edit the same invoice; a dialog *without* them (which is what
 * this was) sent the user back and forth between the list of problems and the fields that fix them.
 *
 * The invoice PDF sits here rather than under the attachments: it is not one file among others but the
 * document the ZUGFeRD export embeds its XML into, and the only reason it exists is this section.
 *
 * Nothing here blocks. An invoice that cannot be exported yet is the ordinary case — that is what the user
 * is on this page for — so the buttons stay pressable and it is the export at the end of them that is
 * refused, after the save that may have fixed it (EInvoiceActions).
 *
 * The checklist is the answer to an export attempt and not a standing verdict on the invoice: most invoices
 * are never exported as an e-invoice, and there is no flag saying which ones will be (the buttons below are
 * the only place that intent is expressed), so naming what an e-invoice of it would still be missing before
 * anyone asked for one reads as an error on an ordinary invoice. Hence the query is on demand and the answer
 * is shown from the first attempt on — and only for as long as it is still about the state it was given for.
 */
export function EInvoiceSection({ id }: { id: number | null }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const isDirty = useStore(
    form.store,
    (s: unknown) => (s as { isDirty: boolean }).isDirty
  );
  /** Whether an export was asked for — nothing before that, see the doc comment above. */
  const [attempted, setAttempted] = useState(false);
  const validation = useQuery({
    queryKey: eInvoiceQueryKey(id),
    queryFn: ({ signal }) => fetchEInvoiceValidation(id!, signal),
    // Only ever fetched through the `revalidate` below, i.e. after a button has asked for an export.
    enabled: false,
    // The list is about the stored invoice and the page can save it without leaving (EInvoiceActions), so a
    // cached answer would keep naming problems that were just fixed.
    staleTime: 0,
  });

  return (
    <div className="mt-6 flex flex-col gap-3 border-t border-border pt-4">
      {/* No note about unsaved changes any more: the two buttons save before they export, so there is no
          state they could leave behind (see EInvoiceActions). Its own hint while the invoice is unsaved. */}
      <InvoicePdfField invoiceId={id} />
      {id == null ? (
        // No checklist and no buttons for an invoice that isn't stored: all three act on the stored one,
        // and the address fields above are what can be filled in already.
        <p className="text-sm text-muted-foreground">
          {t("fibu.rechnung.exportInvoice.onlyStored")}
        </p>
      ) : (
        <>
          {/* Gone again as soon as the form changes: the sentences describe the invoice as it was saved, and
              the fields they name are right above them — so the first keystroke of the answer makes the
              complaint stale. `isDirty` is form-wide, so an edit somewhere else hides it too; that is the
              harmless direction, and pressing a button again brings the current answer back. */}
          {attempted && !isDirty && (
            <EInvoiceChecklist
              errors={validation.data?.errors ?? []}
              isPending={validation.isFetching}
            />
          )}
          {/* `refetch` and not an invalidation: the two buttons act on the answer, so they have to wait
              for it — and it is about the state their own save has just written. */}
          <EInvoiceActions
            invoiceId={id}
            revalidate={async () => {
              setAttempted(true);
              return (await validation.refetch()).data;
            }}
          />
        </>
      )}
    </div>
  );
}
