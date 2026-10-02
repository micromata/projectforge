import { request } from "./client";
import type { FilterListValue } from "./types";

/**
 * The values of a LIST filter whose backend element names a `valuesUrl` (UIFilterListElement) instead of
 * shipping them with the list meta — e.g. the customers of the order book (`order/customerFilterValues`).
 */
export function fetchFilterListValues(
  url: string,
  signal?: AbortSignal
): Promise<FilterListValue[]> {
  return request<FilterListValue[]>(
    `/rs/${url.replace(/^\/+/, "")}`,
    { method: "GET" },
    signal
  );
}
