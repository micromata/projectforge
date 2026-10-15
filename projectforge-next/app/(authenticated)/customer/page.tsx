"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { CUSTOMER_PAGE } from "@/components/features/customer/customer.page";

export default function CustomerListPage() {
  return <EntityListPage page={CUSTOMER_PAGE} />;
}
