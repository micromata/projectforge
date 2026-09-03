"use client";

import { MassUpdatePage } from "@/components/shared/list/mass-update-page";
import { SelectedEntriesPanel } from "@/components/shared/list/selected-entries-panel";
import { CREDITOR_INVOICE_PAGE } from "@/components/features/creditor-invoice/creditor-invoice.page";
import { CreditorInvoiceTransferButton } from "@/components/features/creditor-invoice/creditor-invoice-transfer-button";

/**
 * Reached from the list, never linked directly: the selection this changes lives in the HTTP session,
 * and the list put it there before it routed here (see MassUpdatePage).
 */
export default function CreditorInvoiceMassUpdatePage() {
  const massUpdate = CREDITOR_INVOICE_PAGE.massUpdate!;
  return (
    <MassUpdatePage
      entity={CREDITOR_INVOICE_PAGE.entity}
      massUpdate={massUpdate}
      listRoute={CREDITOR_INVOICE_PAGE.route}
      // The SEPA bank transfer of the whole selection, exactly as the Wicket multi-select page offers it.
      // A page-specific action, so it is passed in rather than known to the generic page.
      actions={<CreditorInvoiceTransferButton selection />}
      // Built here rather than inside the generic page, because it renders the invoice list's own
      // columns — and those are typed, so only the page that declares them can pass them on.
      selectedEntries={(count) => (
        <SelectedEntriesPanel
          endpoint={massUpdate.endpoint}
          metadata={CREDITOR_INVOICE_PAGE.metadata}
          columns={CREDITOR_INVOICE_PAGE.columns}
          // The count is all this page knows of the selection, and it comes from `{page}/meta`, which
          // is refetched on every visit — so a selection changed elsewhere refetches the rows with it.
          selectionKey={String(count)}
          count={count}
        />
      )}
    />
  );
}
