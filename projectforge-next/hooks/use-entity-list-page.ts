"use client";

import {
  DEFAULT_PAGE_SIZE,
  filterValuesFromEntries,
  useColumnStatePersistence,
  useDataTable,
  useFilterFavorites,
  useListFilters,
  useMagicFilterQuery,
  useRememberFilter,
  useTableState,
  type ColumnState,
  type FilterValues,
} from "@/components/data-table";
import type { ColumnDef } from "@tanstack/react-table";
import type { MagicFilter } from "@/lib/rs/types";
import { useInitialList } from "@/hooks/use-initial-list";

/** A row of a list, which only has to be addressable by id. */
export interface ListRow {
  id: number;
}

export interface UseEntityListPageOptions<Row extends ListRow> {
  /** REST category, e.g. `book` — what every list call and the stored state are keyed by. */
  entity: string;
  /** React Query key of the list, e.g. `["book"]`. */
  queryKey: readonly unknown[];
  columns: ColumnDef<Row, unknown>[];
  /** Column layout the server had stored for this user (see useStoredColumnState). */
  storedState: ColumnState;
  /** The filter the user last used, as the backend remembered it. */
  restoredFilter?: MagicFilter;
}

/**
 * Everything a list page is made of besides its columns: the server-side filter query, the sorting,
 * the paging, the search string, the saved filters, and the column layout the backend stores per
 * user and entity.
 *
 * Both pieces of state that seed the table have to be loaded *before* this runs — TanStack's initial
 * state cannot be replaced afterwards without fighting the user's own edits — which is why
 * `storedState` and `restoredFilter` are parameters and the two-phase mount stays with the page (see
 * EntityListPage).
 */
export function useEntityListPage<Row extends ListRow>({
  entity,
  queryKey,
  columns,
  storedState,
  restoredFilter,
}: UseEntityListPageOptions<Row>) {
  const filters = useListFilters(entity, { restoredFilter });
  // Same query as the one behind useListFilters (keyed per entity), so this is a cache read.
  const initialList = useInitialList(entity);

  const columnState = useTableState({
    restoredState: storedState,
    initialSorting: storedState.sorting,
  });

  const query = useMagicFilterQuery<Row>({
    entity,
    queryKey,
    // Stored per entity along with the column state, so the size the user picked survives a reload.
    initialPageSize: storedState.paginationPageSize ?? DEFAULT_PAGE_SIZE,
    // Sorting drives the backend query, so it lives with the query, not in the column state — the
    // stored order seeds it here.
    initialSorting: storedState.sorting,
    // The search box belongs to the filter row, so it is restored with it.
    initialGlobalFilter: restoredFilter?.searchString,
    // The pill filters are applied server-side, unlike the header's column filters.
    filterEntries: filters.entries,
    // Has to go out with every list call: the backend stores the filter it gets as the user's
    // current one, so without it the link to the favorite would be lost and the edited filter could
    // no longer be saved back into it.
    favoriteId: filters.favorite?.id,
    favoriteName: filters.favorite?.name,
  });

  // The user's saved filters — the backend's filter favorites, so a filter saved here is the same
  // one the legacy list page offers.
  const favorites = useFilterFavorites({
    entity,
    filter: query.filter,
    current: filters.favorite,
    onCurrentChange: filters.setFavorite,
    onApply: (applied) => {
      filters.setValues(filterValuesFromEntries(applied.entries));
      query.applyFilter(applied);
    },
  });

  // Owned here so the toolbar's column panel and the table share one instance.
  const table = useDataTable<Row>({
    columns,
    data: query.data,
    rowCount: query.rowCount,
    sorting: query.sorting,
    onSortingChange: query.setSorting,
    pagination: query.pagination,
    onPaginationChange: query.setPagination,
    columnFilters: columnState.columnFilters,
    onColumnFiltersChange: columnState.setColumnFilters,
    columnVisibility: columnState.columnVisibility,
    onColumnVisibilityChange: columnState.setColumnVisibility,
    columnPinning: columnState.columnPinning,
    onColumnPinningChange: columnState.setColumnPinning,
    columnSizing: columnState.columnSizing,
    onColumnSizingChange: columnState.setColumnSizing,
    columnOrder: columnState.columnOrder,
    onColumnOrderChange: columnState.setColumnOrder,
    enableColumnFilters: true,
    enableColumnResizing: true,
    // Sorting and the search string go to Spring; the column filters and paging work on the client,
    // because getList returns the whole result set at once.
    manualSorting: true,
    getRowId: (row: Row) => String(row.id),
  });

  // Coming back to the list should show the filter it was left with, also without a reload — the
  // cached initialList would otherwise still hold the old one. The filter already carries the
  // favorite's id and name.
  useRememberFilter(entity, query.filter);

  useColumnStatePersistence(entity, {
    sorting: query.sorting,
    columnVisibility: columnState.columnVisibility,
    columnPinning: columnState.columnPinning,
    columnSizing: columnState.columnSizing,
    columnOrder: columnState.columnOrder,
    paginationPageSize: query.pagination.pageSize,
  });

  /** Back to the column defs' defaults; the next write stores the empty state. */
  function resetColumns() {
    query.setSorting([]);
    columnState.setColumnVisibility({});
    columnState.setColumnPinning({});
    columnState.setColumnSizing({});
    columnState.setColumnOrder([]);
    columnState.setColumnFilters([]);
  }

  /**
   * Takes the filter row's edits — and drops the link to the saved filter once nothing is left: an
   * empty filter is no longer that favorite, so marking it as modified (and offering to save the
   * emptiness back into it) would be wrong.
   */
  function applyValues(values: FilterValues) {
    filters.setValues(values);
    if (Object.keys(values).length === 0 && !query.globalFilter)
      filters.setFavorite(undefined);
  }

  /**
   * The local half of the gear menu's "reset filter": the endpoint only drops what the server
   * stores. It discards the grid state along with the filter, so the columns go with it.
   */
  function resetFilter() {
    filters.setValues({});
    filters.setFavorite(undefined);
    // Clears search string and sort order and returns to page 1.
    query.applyFilter({ entries: [], sortProperties: [] });
    resetColumns();
  }

  return {
    table,
    filters,
    favorites,
    /** The legacy list page this one replaces (`ui.legacyUrl` of the list response). */
    legacyUrl: initialList.data?.ui?.legacyUrl,
    data: query.data,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    globalFilter: query.globalFilter,
    setGlobalFilter: query.setGlobalFilter,
    resetColumns,
    resetFilter,
    applyValues,
  };
}
