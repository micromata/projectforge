"use client";

import { useQueries, useQuery } from "@tanstack/react-query";
import { fetchFilterListValues } from "@/lib/rs/filter-values";
import type { FilterElement, FilterListValue } from "@/lib/rs/types";
import type { FilterValues } from "./filter-value";

const NO_VALUES: FilterListValue[] = [];

/**
 * The query of a `valuesUrl`. Kept for a few minutes, so reopening the pill doesn't ask again: the set
 * changes with the data (a new customer in the order book), not with the user's typing.
 */
function valuesQuery(url: string | undefined, enabled: boolean) {
  return {
    queryKey: ["filterListValues", url],
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      fetchFilterListValues(url as string, signal),
    enabled: !!url && enabled,
    staleTime: 5 * 60 * 1000,
  };
}

/**
 * The values a LIST filter offers: shipped with the element, or fetched from its `valuesUrl` while
 * [enabled] — the open picker, or a field that has to name what is selected.
 */
export function useFilterListValues(
  element: FilterElement | undefined,
  enabled = true
): { values: FilterListValue[]; loading: boolean } {
  const url = element?.valuesUrl;
  const query = useQuery(valuesQuery(url, enabled));
  if (!url) return { values: element?.values ?? NO_VALUES, loading: false };
  return { values: query.data ?? NO_VALUES, loading: query.isLoading };
}

/**
 * [elements] with the fetched values filled in where a `valuesUrl` element has a selection in [values],
 * so [describeFilterValue] names the picks ("473 - ACME") instead of showing their keys — in a pill and in
 * a chart's summary of the list filter alike. Elements without a selection are not fetched.
 */
export function useResolvedFilterElements(
  elements: readonly FilterElement[],
  values: FilterValues | undefined
): FilterElement[] {
  const lazy = elements.filter(
    (element) => element.valuesUrl && values?.[element.id]?.values?.length
  );
  const results = useQueries({
    queries: lazy.map((element) => valuesQuery(element.valuesUrl, true)),
  });
  return elements.map((element) => {
    const data = results[lazy.indexOf(element)]?.data;
    return data ? { ...element, values: data } : element;
  });
}
