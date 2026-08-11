"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { ORDER_PAGE } from "@/components/features/order/order.page";

export default function OrderListPage() {
  return <EntityListPage page={ORDER_PAGE} />;
}
