"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { INVOICE_PAGE } from "@/components/features/invoice/invoice.page";

export default function InvoiceListPage() {
  return <EntityListPage page={INVOICE_PAGE} />;
}
