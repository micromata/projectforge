import { TAB_PARAM } from "@/components/shared/edit-page-tabs";

/** Set by the order book's buttons: take over its business units, customers and projects once. */
export const FROM_ORDER_BOOK_PARAM = "fromOrderBook";

/** The tabs of the order statistics page; `forecast` is the default. */
export type OrderStatisticsTab = "forecast" | "contributionMargin";

/** The order statistics page, without the basePath (Link and the router prepend it). */
export function orderStatisticsUrl(
  tab: OrderStatisticsTab,
  fromOrderBook = false
): string {
  const query = new URLSearchParams({ [TAB_PARAM]: tab });
  if (fromOrderBook) query.set(FROM_ORDER_BOOK_PARAM, "1");
  return `/finance/statistics?${query}`;
}
