"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import type {
  OnChangeFn,
  PaginationState,
  SortingState,
} from "@tanstack/react-table";
import { fetchList, fetchListPage } from "@/lib/rs/client";
import { paginationPageSizeEntry } from "@/lib/rs/types";
import type { MagicFilter, MagicFilterEntry, ResultSet } from "@/lib/rs/types";
import { DEFAULT_PAGE_SIZE } from "./page-size-options";

interface UseMagicFilterQueryOptions {
  /** Backend entity, e.g. "book" — maps to POST /rs/{entity}/list. */
  entity: string;
  /** React Query key prefix. */
  queryKey: readonly unknown[];
  initialPageSize?: number;
  /**
   * The page the list opens on — the page it was left on, as long as this session remembers it (see
   * recallPageIndex). Nothing else seeds it: a fresh list starts on the first page.
   */
  initialPageIndex?: number;
  initialGlobalFilter?: string;
  initialSorting?: SortingState;
  /**
   * Server-side filter entries (from the filter panel). Passed as data rather
   * than through buildFilter so a caller doesn't have to memoise a callback to
   * avoid refetching on every render.
   */
  filterEntries?: MagicFilterEntry[];
  /**
   * The saved filter the values came from, if any. It has to travel with every
   * list call: the backend stores the filter it receives as the user's current one
   * (saveCurrentFilter), so leaving id/name out would drop the reference to the
   * favorite — the next visit would restore the values without offering to save
   * them back. The backend keeps the id while the values are edited, which is
   * exactly what makes "overwrite this favorite" possible.
   */
  favoriteId?: number;
  favoriteName?: string;
  /** Hook that lets callers customize the MagicFilter before it's sent. */
  buildFilter?: (base: MagicFilter) => MagicFilter;
  enabled?: boolean;
  /**
   * Fetch one server-side page at a time (`listPage`) instead of the whole result set (`list`). Off by
   * default, so an unmigrated page is untouched. A page opts in via `PageDef.serverPaging`.
   */
  serverPaging?: boolean;
  /**
   * A column-header (funnel) filter is set. The funnel narrows on the client, so under it the page
   * falls back to fetching the whole result set (`list`) even when `serverPaging` is on — slower, but
   * the footer total and the narrowing then agree. Without it the page would show "50 of 7000" while
   * the funnel hid rows of the loaded 50 only.
   */
  columnFilterActive?: boolean;
  /**
   * Do not let the backend remember this filter as the user's current one (`ListPageRequest.doNotStore`).
   * For a transient jump into a pre-filtered list — the consumption bar linking to a task's time sheets.
   * Only the server-side page path honours it; the whole-list fallback (`getList`) always stores.
   */
  doNotStore?: boolean;
}

interface UseMagicFilterQueryResult<O> {
  /**
   * The whole result set, not a single page: AbstractPagesRest.getList returns
   * everything up to maxRows. Paging is left to the table, which has to filter
   * before it pages — slicing here would filter the visible page only.
   */
  data: O[];
  rowCount: number;
  /**
   * Aggregates the backend computed over the whole result set, untouched — the sums and counters of the
   * order book (see `ResultSet.statistics` in lib/rs/types.ts). Undefined for an entity that sends none.
   */
  statistics?: unknown;
  /**
   * The row the user edited last, as the backend remembers it per user and category
   * (`AbstractEntityRest.onAfterEdit` writes it, `getList` hands it back). What lets a list mark the
   * entry someone just came back from — see useHighlightedRow.
   */
  highlightRowId?: number;
  /** The filter as sent, so it can be stored as a favorite. */
  filter: MagicFilter;
  isLoading: boolean;
  isFetching: boolean;
  isError: boolean;
  error: unknown;

  sorting: SortingState;
  setSorting: OnChangeFn<SortingState>;
  pagination: PaginationState;
  setPagination: OnChangeFn<PaginationState>;
  globalFilter: string;
  setGlobalFilter: (v: string) => void;
  /**
   * Takes over search string and sort order of a filter the backend handed back
   * (a saved filter that was applied). The field entries are the caller's, since
   * it owns the filter values.
   */
  applyFilter: (filter: MagicFilter) => void;
}

export function useMagicFilterQuery<O>({
  entity,
  queryKey,
  initialPageSize = DEFAULT_PAGE_SIZE,
  initialPageIndex = 0,
  initialGlobalFilter = "",
  initialSorting = [],
  filterEntries,
  favoriteId,
  favoriteName,
  buildFilter,
  enabled = true,
  serverPaging = false,
  columnFilterActive = false,
  doNotStore = false,
}: UseMagicFilterQueryOptions): UseMagicFilterQueryResult<O> {
  const [sorting, setSortingState] = useState<SortingState>(initialSorting);
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: initialPageIndex,
    pageSize: initialPageSize,
  });
  const [globalFilter, setGlobalFilterState] = useState(initialGlobalFilter);

  const setGlobalFilter = (v: string) => {
    setGlobalFilterState(v);
    // Reset to first page on search change.
    setPagination((p) => ({ ...p, pageIndex: 0 }));
  };

  // Sorting drives the backend query, so a new order is a new sequence of rows — the page the user was
  // on may no longer hold the same ones (and under server-side paging a stale high index past the new
  // end would render an empty page). Back to page 1, as a filter or search change does.
  const setSorting: OnChangeFn<SortingState> = (updater) => {
    setSortingState(updater);
    setPagination((p) => ({ ...p, pageIndex: 0 }));
  };

  // Compared by content, then parsed back inside the memo: a caller passing a
  // fresh array each render must not trigger a refetch.
  const serializedEntries = JSON.stringify(filterEntries ?? []);

  // The page index is deliberately not part of the filter: AbstractPagesRest.getList
  // returns the whole result list (capped by maxRows) rather than a single page, so
  // paging happens on the client below. Only the page size is sent, as the entry the
  // backend expects.
  const filter: MagicFilter = useMemo(() => {
    const base: MagicFilter = {
      entries: [
        paginationPageSizeEntry(pagination.pageSize),
        ...(JSON.parse(serializedEntries) as MagicFilterEntry[]),
      ],
      sortProperties: sorting.map((s) => ({
        property: s.id,
        sortOrder: s.desc ? "DESCENDING" : "ASCENDING",
      })),
      searchString: globalFilter || undefined,
      id: favoriteId,
      name: favoriteName,
    };
    return buildFilter ? buildFilter(base) : base;
  }, [
    sorting,
    pagination.pageSize,
    globalFilter,
    serializedEntries,
    favoriteId,
    favoriteName,
    buildFilter,
  ]);

  // Server-side paging only while no column filter is set — the funnel narrows on the client, so it
  // needs the whole result set (see columnFilterActive). The two paths key on different things: the
  // paged one refetches per page, the whole-list one does not.
  const paged = serverPaging && !columnFilterActive;
  const { pageIndex, pageSize } = pagination;

  const query = useQuery<ResultSet<O>>({
    queryKey: paged
      ? [...queryKey, filter, pageIndex, pageSize]
      : [...queryKey, filter],
    queryFn: ({ signal }) =>
      paged
        ? fetchListPage<O>(
            entity,
            filter,
            pageIndex * pageSize,
            pageSize,
            false,
            signal,
            doNotStore
          )
        : fetchList<O>(entity, filter, signal),
    placeholderData: keepPreviousData,
    enabled,
  });

  const allRows = query.data?.resultSet;
  // Stable identity while no result is in: a fresh [] would rebuild the table's rows.
  const rows = useMemo(() => allRows ?? [], [allRows]);

  return {
    data: rows,
    rowCount: query.data?.totalSize ?? rows.length,
    statistics: query.data?.statistics,
    highlightRowId: query.data?.highlightRowId,
    filter,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    isError: query.isError,
    error: query.error,
    sorting,
    setSorting,
    pagination,
    setPagination,
    globalFilter,
    setGlobalFilter,
    applyFilter,
  };

  function applyFilter(applied: MagicFilter) {
    setGlobalFilterState(applied.searchString ?? "");
    setSortingState(
      (applied.sortProperties ?? []).map((property) => ({
        id: property.property,
        desc: property.sortOrder === "DESCENDING",
      }))
    );
    // A different filter means a different result set — page 1 is the only page
    // guaranteed to exist.
    setPagination((p) => ({ ...p, pageIndex: 0 }));
  }
}
