"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { CREDITOR_INVOICE_PAGE } from "@/components/features/creditor-invoice/creditor-invoice.page";

export default function CreditorInvoiceListPage() {
  return <EntityListPage page={CREDITOR_INVOICE_PAGE} />;
}
