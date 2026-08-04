"use client";

import { useState } from "react";
import type {
  ColumnFiltersState,
  ColumnOrderState,
  ColumnPinningState,
  ColumnSizingState,
  SortingState,
  VisibilityState,
} from "@tanstack/react-table";

/**
 * The column state the backend persists per entity category (user prefs, via
 * AbstractPagesRest's setColumnStates). The wire format is TanStack's own state
 * shape, so these slices map 1:1.
 *
 * Note: the backend stores columnFilters but never returns them — it folds the
 * restored state back into the column defs instead (see AGGridSupport).
 */
export interface ColumnState {
  columnOrder?: ColumnOrderState;
  columnSizing?: ColumnSizingState;
  columnVisibility?: VisibilityState;
  columnPinning?: ColumnPinningState;
  sorting?: SortingState;
  columnFilters?: ColumnFiltersState;
}

export interface TableStateOptions {
  initialSorting?: SortingState;
  initialVisibility?: VisibilityState;
  initialPinning?: ColumnPinningState;
  initialSizing?: ColumnSizingState;
  initialOrder?: ColumnOrderState;
}

/** Bundles the column-related state slices shared by DataTable and its toolbars. */
export function useTableState({
  initialSorting = [],
  initialVisibility = {},
  initialPinning = {},
  initialSizing = {},
  initialOrder = [],
}: TableStateOptions = {}) {
  const [sorting, setSorting] = useState<SortingState>(initialSorting);
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [columnVisibility, setColumnVisibility] =
    useState<VisibilityState>(initialVisibility);
  const [columnPinning, setColumnPinning] =
    useState<ColumnPinningState>(initialPinning);
  const [columnSizing, setColumnSizing] =
    useState<ColumnSizingState>(initialSizing);
  const [columnOrder, setColumnOrder] =
    useState<ColumnOrderState>(initialOrder);

  return {
    sorting,
    setSorting,
    columnFilters,
    setColumnFilters,
    columnVisibility,
    setColumnVisibility,
    columnPinning,
    setColumnPinning,
    columnSizing,
    setColumnSizing,
    columnOrder,
    setColumnOrder,
  };
}

export type TableStateResult = ReturnType<typeof useTableState>;
