"use client";

import { Suspense } from "react";
import { OrderStatisticsPage } from "@/components/features/order/statistics/order-statistics-page";

/**
 * The order statistics (`/next/orderStatistics`): forecast and contribution margin over a filter of their
 * own, opened from the menu or the order book's buttons.
 *
 * The `<Suspense>` boundary is required because the page reads `?tab=` and `?fromOrderBook=` via
 * `useSearchParams` under the static export (`output: "export"`).
 */
export default function OrderStatisticsRoute() {
  return (
    <Suspense>
      <OrderStatisticsPage />
    </Suspense>
  );
}
