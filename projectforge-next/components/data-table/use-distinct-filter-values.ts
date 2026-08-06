"use client";

import { useMemo } from "react";
import type { Column } from "@tanstack/react-table";
import { compareFilterText, toFilterText } from "./filter-fns";

/**
 * Above this many distinct values the checkbox list is both slow to build and of
 * little use — one entry per row is a copy of the column, not a filter. The
 * popover then opens on the comparison filter instead; selection stays available.
 */
export const SELECTION_PREFERRED_MAX = 20;

/**
 * How many distinct values a column holds — cheap enough to call while rendering,
 * since it only reads the size of TanStack's faceted map.
 *
 * This counts raw cell values, not the display texts the selection list shows, so
 * it can differ slightly from the list's length (an array cell is one entry but
 * contributes several texts; two objects with the same displayName collapse into
 * one text). Exact would mean iterating everything — the very work the threshold
 * exists to avoid.
 */
export function useDistinctValueCount<TData>(
  column: Column<TData, unknown>
): number {
  return column.getFacetedUniqueValues().size;
}

/**
 * The distinct display texts of a column, sorted for the selection list.
 *
 * Only call this where the list is actually rendered: deduplicating and sorting
 * thousands of values is exactly what makes the popover slow to appear.
 */
export function useDistinctFilterValues<TData>(
  column: Column<TData, unknown>
): string[] {
  const faceted = column.getFacetedUniqueValues();

  // Keyed on the faceted map, not on `column`: the column object keeps its
  // identity for its lifetime, so the list never recomputed when rows changed.
  return useMemo(() => {
    const unique = new Set<string>();
    for (const raw of faceted.keys()) {
      // Array cells contribute each entry separately.
      if (Array.isArray(raw)) raw.forEach((v) => unique.add(toFilterText(v)));
      else unique.add(toFilterText(raw));
    }
    return [...unique].sort(compareFilterText);
  }, [faceted]);
}
