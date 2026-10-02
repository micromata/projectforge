"use client";

import { useQueries, useQuery } from "@tanstack/react-query";
import { fetchFilterListValues } from "@/lib/rs/filter-values";
import type {
  FilterElement,
  FilterListValue,
  MagicFilterEntry,
} from "@/lib/rs/types";
import { filterEntriesOf, type FilterValues } from "./filter-value";
import { useFilterValuesContext } from "./filter-values-context";

const NO_VALUES: FilterListValue[] = [];
const NO_ENTRIES: MagicFilterEntry[] = [];

/**
 * The query of a `valuesUrl`, narrowed by [entries] (see [fetchFilterListValues]). Kept for a few minutes,
 * so reopening the pill doesn't ask again: the set changes with the data (a new customer in the order
 * book) and the other criteria, not with the user's typing. The entries are in a stable order
 * ([filterEntriesOf]), so they key the query as they are.
 */
function valuesQuery(
  url: string | undefined,
  entries: MagicFilterEntry[],
  enabled: boolean
) {
  return {
    queryKey: ["filterListValues", url, entries],
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      fetchFilterListValues(url as string, entries, signal),
    enabled: !!url && enabled,
    staleTime: 5 * 60 * 1000,
  };
}

/** [values] without the entry of [field]: the criteria a checklist on that field is narrowed by. */
export function otherFilterEntries(
  values: FilterValues,
  field: string
): MagicFilterEntry[] {
  return filterEntriesOf(values).filter((entry) => entry.field !== field);
}

/**
 * The values a LIST filter offers: shipped with the element, or fetched from its `valuesUrl` while
 * [enabled] — the open picker, or a field that has to name what is selected.
 *
 * Fetched values are those of the rows the row's *other* criteria match ([useFilterValuesContext]), as
 * Excel's autofilter offers them. The field's own picks are not part of the query, so ticking one doesn't
 * reload the list under the cursor; a pick the others no longer reach is put in front instead, named from
 * the unfiltered answer, so nothing can be ticked but out of sight.
 */
export function useFilterListValues(
  element: FilterElement | undefined,
  enabled = true
): { values: FilterListValue[]; loading: boolean } {
  const url = element?.valuesUrl;
  const filterValues = useFilterValuesContext();
  const entries = element ? otherFilterEntries(filterValues, element.id) : [];
  const picks = (element && filterValues[element.id]?.values) || [];
  const query = useQuery(valuesQuery(url, entries, enabled));
  const offered = query.data ?? NO_VALUES;
  const missing = query.data
    ? picks.filter((key) => !offered.some((it) => it.id === key))
    : [];
  // The same query as the unfiltered naming of [useResolvedFilterElements], so mostly a cache hit.
  const all = useQuery(
    valuesQuery(url, NO_ENTRIES, enabled && missing.length > 0)
  );
  if (!url) return { values: element?.values ?? NO_VALUES, loading: false };
  if (missing.length === 0)
    return { values: offered, loading: query.isLoading };
  const named = missing.map(
    (key) =>
      all.data?.find((it) => it.id === key) ?? { id: key, displayName: key }
  );
  return { values: [...named, ...offered], loading: all.isLoading };
}

/**
 * [elements] with the fetched values filled in where a `valuesUrl` element has a selection in [values],
 * so [describeFilterValue] names the picks ("473 - ACME") instead of showing their keys — in a pill and in
 * a chart's summary of the list filter alike. Elements without a selection are not fetched.
 *
 * Unfiltered: naming needs every value, and one cached answer per url serves every pill.
 */
export function useResolvedFilterElements(
  elements: readonly FilterElement[],
  values: FilterValues | undefined
): FilterElement[] {
  const lazy = elements.filter(
    (element) => element.valuesUrl && values?.[element.id]?.values?.length
  );
  const results = useQueries({
    queries: lazy.map((element) =>
      valuesQuery(element.valuesUrl, NO_ENTRIES, true)
    ),
  });
  return elements.map((element) => {
    const data = results[lazy.indexOf(element)]?.data;
    return data ? { ...element, values: data } : element;
  });
}
