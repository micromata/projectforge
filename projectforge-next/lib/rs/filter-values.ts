import { request } from "./client";
import type { FilterListValue, MagicFilterEntry } from "./types";

/**
 * The values of a LIST filter whose backend element names a `valuesUrl` (UIFilterListElement) instead of
 * shipping them with the list meta — e.g. the customers of the order book (`order/customerFilterValues`).
 *
 * [entries] are the list's other criteria: the backend offers only the values of the rows they match, as
 * Excel's autofilter does. None means every row the user may see.
 */
export function fetchFilterListValues(
  url: string,
  entries: MagicFilterEntry[],
  signal?: AbortSignal
): Promise<FilterListValue[]> {
  return request<FilterListValue[]>(
    `/rs/${url.replace(/^\/+/, "")}`,
    { method: "POST", body: JSON.stringify({ entries }) },
    signal
  );
}
