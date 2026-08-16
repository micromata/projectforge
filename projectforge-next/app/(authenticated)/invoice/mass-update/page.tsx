"use client";

import { MassUpdatePage } from "@/components/shared/list/mass-update-page";
import { INVOICE_PAGE } from "@/components/features/invoice/invoice.page";

/**
 * Reached from the list, never linked directly: the selection this changes lives in the HTTP session,
 * and the list put it there before it routed here (see MassUpdatePage).
 */
export default function InvoiceMassUpdatePage() {
  return (
    <MassUpdatePage
      entity={INVOICE_PAGE.entity}
      massUpdate={INVOICE_PAGE.massUpdate!}
      listRoute={INVOICE_PAGE.route}
    />
  );
}
