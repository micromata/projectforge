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
  type Row,
  type RowSelectionState,
  type SortingState,
  type Table,
  type VisibilityState,
} from "@tanstack/react-table";
import {
  columnIdOfDef,
  withLockedFirst,
  withPinnedFirst,
} from "./column-order";
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
   * Columns that lead the table whatever the user's stored layout says — the selection checkbox.
   *
   * Folded into the order and the left pinning at render time only, so nothing of it reaches the state
   * the caller persists (see withLockedFirst for why the order alone would not do).
   */
  lockedColumnIds?: string[];
  /**
   * Which rows are picked, keyed by row id — held by the caller, because the selection outlives the
   * table: it is posted to the backend and read by a toolbar (see useRowSelection).
   */
  rowSelection?: RowSelectionState;
  onRowSelectionChange?: OnChangeFn<RowSelectionState>;
  /**
   * Whether rows can be picked at all; only a page with a mass update switches this on. A predicate
   * gates it per row — the import preview lets only importable statuses be ticked.
   */
  enableRowSelection?: boolean | ((row: Row<TData>) => boolean);

  /** Column filters are applied client-side; the backend returns the full result set. */
  enableColumnFilters?: boolean;
  enableColumnResizing?: boolean;

  manualSorting?: boolean;
  manualPagination?: boolean;
  manualFiltering?: boolean;

  getRowId?: (row: TData, index: number) => string;
  initialPageSize?: number;
  /**
   * The active search term, exposed to every cell through the table meta so text cells can highlight
   * where it matched (see TableMeta.highlight). Left unset where there is no term to highlight.
   */
  highlight?: string;
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
  lockedColumnIds,
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
  highlight,
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

  const allIds = useMemo(() => columns.map(columnIdOfDef), [columns]);
  // Only the locked columns that are actually declared: the selection column is added to `columns` for
  // the duration of the selection mode, while the id stays passed in.
  const lockedIds = useMemo(
    () => (lockedColumnIds ?? []).filter((id) => allIds.includes(id)),
    [lockedColumnIds, allIds]
  );

  // A locked column leads the table, so it is left-pinned whatever the user's stored pinning says —
  // frozen like the columns that identify the row, and ahead of them.
  const effectivePinning = useMemo(() => {
    const pinning = columnPinning ?? internalPinning;
    if (!lockedIds.length) return pinning;
    return {
      ...pinning,
      left: [
        ...lockedIds,
        ...(pinning.left ?? []).filter((id) => !lockedIds.includes(id)),
      ],
    };
  }, [columnPinning, internalPinning, lockedIds]);

  // The order the table renders in — not the one the caller holds and persists: a pinned column has
  // to be rendered in its pinning group, or its sticky offset and its slot in the row disagree and the
  // pinned columns overlap (see withPinnedFirst). The locked ones go first, before that fix-up runs, so
  // withPinnedFirst finds them in the order it works over (it drops a pinned id the order lacks).
  const effectiveOrder = useMemo(
    () =>
      withPinnedFirst(
        withLockedFirst(columnOrder ?? internalOrder, lockedIds, allIds),
        effectivePinning,
        allIds
      ),
    [columnOrder, internalOrder, lockedIds, effectivePinning, allIds]
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
    meta: { highlight },
  });
}
