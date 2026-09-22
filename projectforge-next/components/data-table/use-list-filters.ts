"use client";

import { useMemo, useState } from "react";
import { useListMeta } from "@/hooks/use-list-meta";
import { useFormatContext } from "@/hooks/use-format";
import { filterElementsOf } from "@/lib/rs/filter-elements";
import type {
  FilterElement,
  MagicFilter,
  MagicFilterEntry,
} from "@/lib/rs/types";
import {
  filterEntriesOf,
  filterValuesFromEntries,
  type FilterValues,
} from "./filter-value";
import { refreshedPeriodValues } from "./filter-period";

export interface UseListFiltersOptions {
  /**
   * The filter the backend has stored for this user (from `listMeta`). Its
   * field entries seed the values, so coming back to the list shows the filter
   * the user left it with.
   *
   * Like the column state it has to be there on the first render — swapping it in
   * afterwards would overwrite what the user has meanwhile typed — so the caller
   * holds the list back until it has arrived.
   */
  restoredFilter?: MagicFilter;
}

/**
 * The saved filter the current values came from. It stays set while the user edits
 * them — that is what lets the edited filter be written back to that favorite. The
 * backend does the same (it keeps `id` on the stored current filter), so it means
 * "based on" rather than "identical to".
 */
export interface FavoriteRef {
  id: number;
  name?: string;
}

export interface UseListFiltersResult {
  /** The filter fields the backend offers for this entity. */
  elements: FilterElement[];
  values: FilterValues;
  setValues: (values: FilterValues) => void;
  /** `values` in the shape MagicFilter.entries expects. */
  entries: MagicFilterEntry[];
  favorite: FavoriteRef | undefined;
  setFavorite: (favorite: FavoriteRef | undefined) => void;
}

/**
 * The server-side filter fields of a list page and their current values.
 *
 * Which fields exist is decided by the backend per entity (derived from the DAO's
 * search fields), so they come from `listMeta` rather than from the frontend.
 * The values are the caller's, because they feed the list query.
 *
 * Saved filters are a separate hook ([useFilterFavorites]): they need the filter
 * the query built from these values, which only exists after the query is set up.
 */
export function useListFilters(
  entity: string,
  { restoredFilter }: UseListFiltersOptions = {}
): UseListFiltersResult {
  const ctx = useFormatContext();
  // Once, on the first render, and then the values are the user's: a period given as "bis heute" is
  // recomputed for the day the list is opened on, so a filter left as 01.11.2025–22.08.2026 asks about
  // 01.11.2025–23.08.2026 tomorrow (see [refreshedPeriodValues]). Every other bound is restored as it was
  // stored.
  const [values, setValues] = useState<FilterValues>(() =>
    refreshedPeriodValues(filterValuesFromEntries(restoredFilter?.entries), ctx)
  );
  // Lives here, not in useFilterFavorites: the id has to travel with the filter
  // the query builds, and that query is set up before the favorites hook.
  const [favorite, setFavorite] = useState<FavoriteRef | undefined>(() =>
    restoredFilter?.id
      ? { id: restoredFilter.id, name: restoredFilter.name }
      : undefined
  );
  const meta = useListMeta(entity);

  const elements = useMemo(() => filterElementsOf(meta.data), [meta.data]);
  const entries = useMemo(() => filterEntriesOf(values), [values]);

  return { elements, values, setValues, entries, favorite, setFavorite };
}
