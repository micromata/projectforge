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
  useRememberPageIndex,
  recallPageIndex,
  refreshedPeriodValues,
  useTableState,
  type ColumnState,
  type FilterValues,
} from "@/components/data-table";
import { useFormatContext } from "@/hooks/use-format";
import { useCallback, useEffect, useRef } from "react";
import type {
  ColumnDef,
  ColumnPinningState,
  Table as TanstackTable,
  VisibilityState,
} from "@tanstack/react-table";
import type { MagicFilter } from "@/lib/rs/types";
import { useListMeta } from "@/hooks/use-list-meta";
import { useListSelection } from "@/hooks/use-list-selection";

/**
 * A row of a list: addressable by id, and marked as deleted or not.
 *
 * `deleted` comes from `BaseDTO` and every entity carries it; it is optional here only because
 * `JsonInclude.NON_NULL` lets the server omit it for a row that isn't deleted.
 */
export interface ListRow {
  id: number;
  deleted?: boolean;
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
  /**
   * Columns frozen to an edge as long as the user hasn't pinned anything themselves — the starting
   * point of the list and what its reset returns to (see `defaultPinningOf`).
   */
  defaultPinning?: ColumnPinningState;
  /**
   * Columns hidden until the user asks for them — the two audit columns every list offers (see
   * `auditColumnsFor`). Per column, so a stored visibility overrides only what the user themselves
   * switched.
   */
  defaultVisibility?: VisibilityState;
  /**
   * REST base of the entity's mass update page (`invoiceSelected`), for a list that offers one — see
   * `MassUpdateDef.endpoint`, and useListSelection for what it is called with.
   *
   * What switches the selection mode on is the user, not this: absent it there is no mode to enter,
   * because a selection nothing can be done with is only a way to stop a click from opening a row.
   */
  massUpdateEndpoint?: string;
  /**
   * Columns that lead the table whatever the user's stored layout says — the selection checkbox (see
   * withLockedFirst).
   */
  lockedColumnIds?: string[];
  /**
   * Hook to customise the MagicFilter before it is sent — the page's own view options that are not
   * filter pills (e.g. the invoice list's previous-year comparison, which adds `extended` flags). The
   * built filter is part of the query key, so a change here refetches on its own.
   */
  buildFilter?: (base: MagicFilter) => MagicFilter;
  /**
   * Fetch the list one server-side page at a time (see `PageDef.serverPaging`). When a column-header
   * filter is set the page falls back to fetching the whole result set, so that funnel still narrows
   * the *whole* list rather than the loaded page (see useMagicFilterQuery.columnFilterActive).
   */
  serverPaging?: boolean;
  /**
   * The list was opened by a transient jump into a pre-filtered view (the consumption bar → a task's
   * time sheets). Its filter must not stick as the user's remembered one — neither on the backend
   * (`ListPageRequest.doNotStore`) nor in the local `listMeta` cache (see useRememberFilter).
   */
  transient?: boolean;
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
  defaultPinning,
  defaultVisibility,
  massUpdateEndpoint,
  lockedColumnIds,
  buildFilter,
  serverPaging = false,
  transient = false,
}: UseEntityListPageOptions<Row>) {
  const ctx = useFormatContext();
  const filters = useListFilters(entity, { restoredFilter });
  // Same query as the one behind useListFilters (keyed per entity), so this is a cache read.
  const meta = useListMeta(entity);

  const columnState = useTableState({
    restoredState: storedState,
    initialSorting: storedState.sorting,
    // Only for a user who has never pinned anything: a stored `{}` means they unpinned every column,
    // and that decision has to survive a reload.
    initialPinning: defaultPinning,
    // Merged per column with the stored visibility, unlike the pinning — see useTableState.
    initialVisibility: defaultVisibility,
  });

  // The funnel filters on the client, so once one is set the page has to hold the whole result set for
  // it to narrow — server-side paging falls back to fetching everything (see useMagicFilterQuery). Both
  // the query (which fetch) and the table (whether TanStack pages) read this, so they stay in step.
  const columnFilterActive = columnState.columnFilters.length > 0;
  const pagedFromServer = serverPaging && !columnFilterActive;

  const query = useMagicFilterQuery<Row>({
    entity,
    queryKey,
    serverPaging,
    columnFilterActive,
    // Stored per entity along with the column state, so the size the user picked survives a reload.
    initialPageSize: storedState.paginationPageSize ?? DEFAULT_PAGE_SIZE,
    // The page the list was left on, so opening an entry and coming back does not start over at the
    // first one. Only for as long as the document lives, unlike the page size (see recallPageIndex).
    initialPageIndex: recallPageIndex(entity),
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
    // The page's non-pill view options (see UseEntityListPageOptions.buildFilter).
    buildFilter,
    // A transient jump must not leave its filter behind as the user's remembered one.
    doNotStore: transient,
  });

  // The user's saved filters — the backend's filter favorites, so a filter saved here is the same
  // one the legacy list page offers.
  const favorites = useFilterFavorites({
    entity,
    filter: query.filter,
    current: filters.favorite,
    onCurrentChange: filters.setFavorite,
    onApply: (applied) => {
      // A period given as "bis heute" is brought up to the day the favorite is applied, the same
      // refresh the first mount does when the list seeds from the stored filter (see [useListFilters]).
      // Without it a saved "Jahr bis heute" would apply the bounds frozen at save time — 24.08 today
      // still says 24.08 — since one of the arts cannot be read back off its two dates.
      filters.setValues(
        refreshedPeriodValues(filterValuesFromEntries(applied.entries), ctx)
      );
      query.applyFilter(applied);
    },
  });

  // The selection has to exist before the table, because the table renders it, while its ranges are
  // taken over the rows the table displays — so it reads them back through this box, which is filled
  // after the table below is built. A ref rather than a second render pass: nothing renders from it,
  // it is only read inside an event handler, by which time the effect has long run.
  const tableRef = useRef<TanstackTable<Row> | null>(null);
  const displayedRowIds = useCallback(
    () => (tableRef.current?.getRowModel().rows ?? []).map((row) => row.id),
    []
  );
  const selectionMode = useListSelection({
    entity,
    endpoint: massUpdateEndpoint,
    filter: query.filter,
    // What the session still had ticked; restored once, and only into a store that knows nothing yet.
    restoredIds: meta.data?.selectedIds,
    displayedRowIds,
  });
  const selection = selectionMode.selection;

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
    lockedColumnIds,
    rowSelection: selection?.state,
    onRowSelectionChange: selection?.setState,
    // Only inside the selection mode: with it on, the header checkbox and `row.getIsSelected()` mean
    // something, and outside it there is nothing to render them into.
    enableRowSelection: selectionMode.active,
    enableColumnFilters: true,
    enableColumnResizing: true,
    // Sorting and the search string always go to Spring. Paging is the server's only while a page opts
    // into it and no funnel is set: then `data` is the 50-row page and TanStack must not page it again.
    // With a funnel set (or an unmigrated page) the client holds the whole result set and pages it, the
    // way it always did. The two switch together with the fetch mode above so they can never disagree.
    manualSorting: true,
    manualPagination: pagedFromServer,
    getRowId: (row: Row) => String(row.id),
  });
  useEffect(() => {
    tableRef.current = table;
  }, [table]);

  // Coming back to the list should show the filter it was left with, also without a reload — the
  // cached listMeta would otherwise still hold the old one. The filter already carries the
  // favorite's id and name.
  // Off for a transient jump, so its task filter is not remembered locally either.
  useRememberFilter(entity, query.filter, !transient);

  // The other half of the page memory: records where the user is, and clamps a remembered page the
  // result set no longer has.
  useRememberPageIndex(
    entity,
    table,
    query.pagination.pageIndex,
    !query.isFetching
  );

  useColumnStatePersistence(entity, {
    sorting: query.sorting,
    columnVisibility: columnState.columnVisibility,
    columnPinning: columnState.columnPinning,
    columnSizing: columnState.columnSizing,
    columnOrder: columnState.columnOrder,
    paginationPageSize: query.pagination.pageSize,
  });

  /**
   * Back to the column defs' defaults: an empty order is the order they are declared in, an empty
   * sizing is "as declared", and the pinning and the visibility are the ones the page declares — the
   * two slices with a default other than "nothing", so they are set rather than cleared.
   */
  function resetColumns() {
    query.setSorting([]);
    columnState.setColumnVisibility(defaultVisibility ?? {});
    columnState.setColumnPinning(defaultPinning ?? {});
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
    /**
     * The selection mode: whether it is on, what is ticked, and how to enter and leave it (see
     * useListSelection). `selection` inside it is undefined while the mode is off, which is what
     * keeps the checkbox column, the keyboard and the selecting click out of a plain list.
     */
    selectionMode,
    /** The legacy list page this one replaces; undefined once it is gone (see ListMetaData). */
    legacyUrl: meta.data?.legacyListPage,
    data: query.data,
    /**
     * The result was capped by the backend's row limit, so more rows match than came back
     * (see useMagicFilterQuery.truncated). Drives the toolbar's red truncation notice; `rowCount`
     * is the cap that was hit.
     */
    truncated: query.truncated,
    rowCount: query.rowCount,
    /**
     * The backend's markdown note about the result (`ResultSet.resultInfo`), shown under the table —
     * for a hand built list the red truncation span (see ListResultInfo). Undefined when there is none.
     */
    resultInfo: query.resultInfo,
    /**
     * The MagicFilter exactly as the list call sends it — what a list-level action has to post to act on
     * the same rows the table shows (see PageDef.listActions and the order book's exports).
     */
    filter: query.filter,
    /** What the backend aggregated over the result set, for a page that shows it (see PageDef.statistics). */
    statistics: query.statistics,
    /** The entry the user edited last, which the list marks and scrolls to (see useHighlightedRow). */
    highlightRowId: query.highlightRowId,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    /**
     * The list call failed. Passed on because an empty table is the one thing a caller must *not* show
     * for it: a 403 here means the backend refused the read (see useReadAccessGuard).
     */
    isError: query.isError,
    error: query.error,
    globalFilter: query.globalFilter,
    setGlobalFilter: query.setGlobalFilter,
    resetColumns,
    resetFilter,
    applyValues,
  };
}
