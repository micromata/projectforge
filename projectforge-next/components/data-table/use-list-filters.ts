"use client";

import { useMemo, useState } from "react";
import { useInitialList } from "@/hooks/use-initial-list";
import { filterElementsOf } from "@/lib/rs/filter-elements";
import type { FilterElement, MagicFilterEntry } from "@/lib/rs/types";
import { filterEntriesOf, type FilterValues } from "./filter-value";

export interface UseListFiltersResult {
  /** The filter fields the backend offers for this entity. */
  elements: FilterElement[];
  values: FilterValues;
  setValues: (values: FilterValues) => void;
  /** `values` in the shape MagicFilter.entries expects. */
  entries: MagicFilterEntry[];
}

/**
 * The server-side filter fields of a list page and their current values.
 *
 * Which fields exist is decided by the backend per entity (derived from the DAO's
 * search fields), so they come from the list layout rather than from the frontend.
 * The values are the caller's, because they feed the list query.
 *
 * Saved filters are a separate hook ([useFilterFavorites]): they need the filter
 * the query built from these values, which only exists after the query is set up.
 */
export function useListFilters(entity: string): UseListFiltersResult {
  const [values, setValues] = useState<FilterValues>({});
  const layout = useInitialList(entity);

  const elements = useMemo(
    () => filterElementsOf(layout.data?.ui),
    [layout.data]
  );
  const entries = useMemo(() => filterEntriesOf(values), [values]);

  return { elements, values, setValues, entries };
}
