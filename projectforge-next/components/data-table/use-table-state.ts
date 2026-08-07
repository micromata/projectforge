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
 * AbstractPagesRest's setColumnStates / columnStates). The wire format is
 * TanStack's own state shape, so these slices map 1:1.
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
  /**
   * State previously stored for this user (e.g. from the backend), used as the
   * starting point. It has to be present on the first render — state can't be
   * swapped in later without fighting the user's edits — so the caller should
   * hold the table back until it has loaded.
   */
  restoredState?: ColumnState;
}

/** Bundles the column-related state slices shared by DataTable and its toolbars. */
export function useTableState({
  initialSorting = [],
  initialVisibility = {},
  initialPinning = {},
  initialSizing = {},
  initialOrder = [],
  restoredState,
}: TableStateOptions = {}) {
  // Each slice of the stored state is optional: the backend omits whatever the
  // user never changed. columnFilters are deliberately not restored: they work on
  // the client, sit hidden inside the column headers, and would filter a reopened
  // list without anything on screen saying so. The filter row is the persisted
  // surface (see useListFilters) — that one is visible as pills.
  const [sorting, setSorting] = useState<SortingState>(
    restoredState?.sorting?.length ? restoredState.sorting : initialSorting
  );
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [columnVisibility, setColumnVisibility] = useState<VisibilityState>(
    restoredState?.columnVisibility ?? initialVisibility
  );
  const [columnPinning, setColumnPinning] = useState<ColumnPinningState>(
    restoredState?.columnPinning ?? initialPinning
  );
  const [columnSizing, setColumnSizing] = useState<ColumnSizingState>(
    restoredState?.columnSizing ?? initialSizing
  );
  const [columnOrder, setColumnOrder] = useState<ColumnOrderState>(
    restoredState?.columnOrder?.length
      ? restoredState.columnOrder
      : initialOrder
  );

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
