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
    },
    onSortingChange: onSortingChange ?? setInternalSorting,
    onPaginationChange: onPaginationChange ?? setInternalPagination,
    onColumnFiltersChange: onColumnFiltersChange ?? setInternalFilters,
    onColumnVisibilityChange: onColumnVisibilityChange ?? setInternalVisibility,
    onColumnPinningChange: onColumnPinningChange ?? setInternalPinning,
    onColumnSizingChange: onColumnSizingChange ?? setInternalSizing,
    onColumnOrderChange: onColumnOrderChange ?? setInternalOrder,
    getRowId,
  });
}
