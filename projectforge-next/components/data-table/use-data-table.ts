"use client";

import { useMemo, useState } from "react";
import {
  getCoreRowModel,
  getFacetedRowModel,
  getFacetedUniqueValues,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
  type ColumnDef,
  type ColumnFiltersState,
  type ColumnOrderState,
  type ColumnPinningState,
  type ColumnSizingState,
  type OnChangeFn,
  type PaginationState,
  type RowSelectionState,
  type SortingState,
  type Table,
  type VisibilityState,
} from "@tanstack/react-table";
import { columnIdOfDef, withPinnedFirst } from "./column-order";
import { universalFilterFnFor } from "./filter-fns";
import { DEFAULT_PAGE_SIZE } from "./page-size-options";

export interface UseDataTableOptions<TData> {
  columns: ColumnDef<TData, unknown>[];
  data: TData[];
  /** Total row count (server-side). Required when manualPagination is true. */
  rowCount?: number;

  sorting?: SortingState;
  onSortingChange?: OnChangeFn<SortingState>;
  pagination?: PaginationState;
  onPaginationChange?: OnChangeFn<PaginationState>;
  columnFilters?: ColumnFiltersState;
  onColumnFiltersChange?: OnChangeFn<ColumnFiltersState>;
  columnVisibility?: VisibilityState;
  onColumnVisibilityChange?: OnChangeFn<VisibilityState>;
  columnPinning?: ColumnPinningState;
  onColumnPinningChange?: OnChangeFn<ColumnPinningState>;
  columnSizing?: ColumnSizingState;
  onColumnSizingChange?: OnChangeFn<ColumnSizingState>;
  columnOrder?: ColumnOrderState;
  onColumnOrderChange?: OnChangeFn<ColumnOrderState>;
  /**
   * Which rows are picked, keyed by row id — held by the caller, because the selection outlives the
   * table: it is posted to the backend and read by a toolbar (see useRowSelection).
   */
  rowSelection?: RowSelectionState;
  onRowSelectionChange?: OnChangeFn<RowSelectionState>;
  /** Whether rows can be picked at all; only a page with a mass update switches this on. */
  enableRowSelection?: boolean;

  /** Column filters are applied client-side; the backend returns the full result set. */
  enableColumnFilters?: boolean;
  enableColumnResizing?: boolean;

  manualSorting?: boolean;
  manualPagination?: boolean;
  manualFiltering?: boolean;

  getRowId?: (row: TData, index: number) => string;
  initialPageSize?: number;
}

/**
 * Creates the TanStack table instance.
 *
 * Callers own the instance so that a toolbar (column panel) and the table itself
 * render from the same one. Reading it out of `DataTable` via a callback would
 * always hand out the previous render's instance, leaving the toolbar one step
 * behind the table.
 */
export function useDataTable<TData>({
  columns,
  data,
  rowCount,
  sorting,
  onSortingChange,
  pagination,
  onPaginationChange,
  columnFilters,
  onColumnFiltersChange,
  columnVisibility,
  onColumnVisibilityChange,
  columnPinning,
  onColumnPinningChange,
  columnSizing,
  onColumnSizingChange,
  columnOrder,
  onColumnOrderChange,
  rowSelection,
  onRowSelectionChange,
  enableRowSelection = false,
  enableColumnFilters = false,
  enableColumnResizing = false,
  manualSorting = false,
  manualPagination = false,
  manualFiltering = false,
  getRowId,
  initialPageSize = DEFAULT_PAGE_SIZE,
}: UseDataTableOptions<TData>): Table<TData> {
  const [internalSorting, setInternalSorting] = useState<SortingState>([]);
  const [internalPagination, setInternalPagination] = useState<PaginationState>(
    {
      pageIndex: 0,
      pageSize: initialPageSize,
    }
  );
  const [internalFilters, setInternalFilters] = useState<ColumnFiltersState>(
    []
  );
  const [internalVisibility, setInternalVisibility] = useState<VisibilityState>(
    {}
  );
  const [internalPinning, setInternalPinning] = useState<ColumnPinningState>(
    {}
  );
  const [internalSizing, setInternalSizing] = useState<ColumnSizingState>({});
  const [internalOrder, setInternalOrder] = useState<ColumnOrderState>([]);

  const effectivePinning = columnPinning ?? internalPinning;
  // The order the table renders in — not the one the caller holds and persists: a pinned column has
  // to be rendered in its pinning group, or its sticky offset and its slot in the row disagree and the
  // pinned columns overlap (see withPinnedFirst).
  const effectiveOrder = useMemo(
    () =>
      withPinnedFirst(
        columnOrder ?? internalOrder,
        effectivePinning,
        columns.map(columnIdOfDef)
      ),
    [columnOrder, internalOrder, effectivePinning, columns]
  );

  const changePagination = onPaginationChange ?? setInternalPagination;
  /**
   * A column filter changes which rows exist, so the page the user was on may be gone — page 1 is the
   * only one sure to be there. Done here rather than by TanStack's `autoResetPageIndex` (switched off
   * above), which cannot tell a new set of rows from the same rows fetched again.
   */
  const changeColumnFilters: OnChangeFn<ColumnFiltersState> = (updater) => {
    (onColumnFiltersChange ?? setInternalFilters)(updater);
    changePagination((previous) => ({ ...previous, pageIndex: 0 }));
  };

  return useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: manualSorting ? undefined : getSortedRowModel(),
    getPaginationRowModel: manualPagination
      ? undefined
      : getPaginationRowModel(),
    // Column filters always work on the client: the backend returns the whole
    // result set, so there is nothing to round-trip for them.
    getFilteredRowModel: enableColumnFilters
      ? getFilteredRowModel()
      : undefined,
    getFacetedRowModel: enableColumnFilters ? getFacetedRowModel() : undefined,
    getFacetedUniqueValues: enableColumnFilters
      ? getFacetedUniqueValues()
      : undefined,
    defaultColumn: {
      minSize: 50,
      size: 150,
      filterFn: universalFilterFnFor<TData>(),
    },
    enableColumnResizing,
    columnResizeMode: "onChange",
    // TanStack resets the page index whenever the data array is replaced, which here happens on every
    // refetch of an unchanged list — the user would be thrown back to page 1 by a background refresh,
    // and the jump to the last-edited row (see useHighlightedRow) would be undone a moment after it
    // happened. The case that really does need page 1 is a *changed* result set, which is reset
    // deliberately: for the server-side filters in useMagicFilterQuery, and for the client-side column
    // filters above.
    autoResetPageIndex: false,
    manualSorting,
    manualPagination,
    manualFiltering,
    rowCount: manualPagination ? rowCount : undefined,
    state: {
      sorting: sorting ?? internalSorting,
      pagination: pagination ?? internalPagination,
      columnFilters: columnFilters ?? internalFilters,
      columnVisibility: columnVisibility ?? internalVisibility,
      columnPinning: effectivePinning,
      columnSizing: columnSizing ?? internalSizing,
      columnOrder: effectiveOrder,
      rowSelection: rowSelection ?? {},
    },
    enableRowSelection,
    onSortingChange: onSortingChange ?? setInternalSorting,
    onPaginationChange: changePagination,
    onColumnFiltersChange: changeColumnFilters,
    onColumnVisibilityChange: onColumnVisibilityChange ?? setInternalVisibility,
    onColumnPinningChange: onColumnPinningChange ?? setInternalPinning,
    onColumnSizingChange: onColumnSizingChange ?? setInternalSizing,
    onColumnOrderChange: onColumnOrderChange ?? setInternalOrder,
    onRowSelectionChange,
    getRowId,
  });
}
