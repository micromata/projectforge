"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { ORDER_PAGE } from "@/components/features/order/order.page";
import { TAB_PARAM } from "@/components/shared/edit-page-tabs";
import {
  orderStatisticsUrl,
  type OrderStatisticsTab,
} from "@/components/features/order/statistics/order-statistics-url";

/** The tabs the order book had before forecast and contribution margin moved to `/finance/statistics`. */
const MOVED_TABS: Record<string, OrderStatisticsTab> = {
  charts: "forecast",
  contributionMargin: "contributionMargin",
};

/**
 * The order book (`/order`). Forecast and contribution margin are a page of their own, the order
 * statistics, opened by the list's buttons (see OrderListActions); a bookmark of their former tab here
 * (`?tab=charts|contributionMargin`) is sent there.
 */
export default function OrderListPage() {
  const router = useRouter();
  const movedTab = MOVED_TABS[useSearchParams().get(TAB_PARAM) ?? ""];
  useEffect(() => {
    if (movedTab) router.replace(orderStatisticsUrl(movedTab));
  }, [movedTab, router]);
  return <EntityListPage page={ORDER_PAGE} />;
}
