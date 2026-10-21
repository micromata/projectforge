"use client";

import { createContext, useContext } from "react";
import type { FilterValues } from "./filter-value";

const NO_VALUES: FilterValues = {};

/**
 * The filter the fields of a filter row are edited against: the applied values on the pill row, the draft
 * in the "all filters" dialog.
 *
 * Read by a checklist field whose values the backend loads (`valuesUrl`), which offers only what the
 * *other* criteria leave, as Excel's autofilter does — the customers of the projects already chosen (see
 * [useFilterListValues]). A context, because the field sits levels below the row and nothing between has
 * anything to do with the other fields' values. Without a provider the values are empty: unfiltered.
 */
const FilterValuesContext = createContext<FilterValues>(NO_VALUES);

export const FilterValuesProvider = FilterValuesContext.Provider;

export function useFilterValuesContext(): FilterValues {
  return useContext(FilterValuesContext);
}

/**
 * The list's search term, the criterion beside the filter values: a checklist narrowed by the other
 * criteria is narrowed by it too, or it would offer what the list doesn't show. Without a provider none.
 */
const FilterSearchStringContext = createContext<string | undefined>(undefined);

export const FilterSearchStringProvider = FilterSearchStringContext.Provider;

export function useFilterSearchString(): string | undefined {
  return useContext(FilterSearchStringContext);
}
