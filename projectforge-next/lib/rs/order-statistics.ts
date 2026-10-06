/**
 * The filter and the favorites of the order statistics page (`/finance/statistics`), see
 * `OrderStatisticsRest`. The charts themselves are fetched by `fetchForecastChart` and
 * `fetchContributionMargin` (`./order`), which store the filter they are asked for as the current one.
 *
 * The filter holds business units, customers and projects only (`OrderStatisticsFilterService`); it is
 * independent of the order book's filter, which it takes over once when opened from there
 * (`fromOrderBook`).
 */

import { request } from "./client";
import type { FavoriteIdTitle, FilterElement, MagicFilter } from "./types";

export interface OrderStatisticsMeta {
  /** The business unit (if any configured), customer and project filter. */
  filterElements: FilterElement[];
  /** The current filter; `id`/`name` name the favorite it came from, if any. */
  filter: MagicFilter;
  /** The favorite `filter.id` refers to, as saved — the baseline that tells whether the filter was modified. */
  favorite?: MagicFilter | null;
  filterFavorites: FavoriteIdTitle[];
  /** Whether the user may see the contribution margin. */
  contributionMargin: boolean;
}

/** Answer of every `filter/*` endpoint: the current filter and the favorites after the change. */
export interface OrderStatisticsFavoritesResponse {
  filter: MagicFilter;
  filterFavorites: FavoriteIdTitle[];
}

/**
 * @param fromOrderBook Opened from the order book: the business units, customers and projects of its
 *   current filter replace the statistics filter.
 */
export function fetchOrderStatisticsMeta(
  fromOrderBook: boolean,
  signal?: AbortSignal
): Promise<OrderStatisticsMeta> {
  return request<OrderStatisticsMeta>(
    `/rs/orderStatistics/meta${fromOrderBook ? "?fromOrderBook=true" : ""}`,
    { method: "GET" },
    signal
  );
}

function post(
  path: string,
  body?: MagicFilter
): Promise<OrderStatisticsFavoritesResponse> {
  return request<OrderStatisticsFavoritesResponse>(
    `/rs/orderStatistics/filter/${path}`,
    { method: "POST", body: body ? JSON.stringify(body) : undefined },
    undefined
  );
}

export function selectOrderStatisticsFavorite(id: number) {
  return post(`select?id=${id}`);
}

/** Saves the filter under `filter.name` (a free one, if taken). */
export function createOrderStatisticsFavorite(filter: MagicFilter) {
  return post("create", filter);
}

/** Overwrites the favorite `filter.id` with the criteria of `filter`. */
export function updateOrderStatisticsFavorite(filter: MagicFilter) {
  return post("update", filter);
}

export function renameOrderStatisticsFavorite(id: number, newName: string) {
  return post(`rename?id=${id}&newName=${encodeURIComponent(newName)}`);
}

export function deleteOrderStatisticsFavorite(id: number) {
  return post(`delete?id=${id}`);
}
