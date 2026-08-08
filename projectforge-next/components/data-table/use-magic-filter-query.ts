"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import type {
  OnChangeFn,
  PaginationState,
  SortingState,
} from "@tanstack/react-table";
import { fetchList } from "@/lib/rs/client";
import { paginationPageSizeEntry } from "@/lib/rs/types";
import type { MagicFilter, MagicFilterEntry, ResultSet } from "@/lib/rs/types";
import { DEFAULT_PAGE_SIZE } from "./page-size-options";

interface UseMagicFilterQueryOptions {
  /** Backend entity, e.g. "book" — maps to POST /rs/{entity}/list. */
  entity: string;
  /** React Query key prefix. */
  queryKey: readonly unknown[];
  initialPageSize?: number;
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
}

interface UseMagicFilterQueryResult<O> {
  /**
   * The whole result set, not a single page: AbstractPagesRest.getList returns
   * everything up to maxRows. Paging is left to the table, which has to filter
   * before it pages — slicing here would filter the visible page only.
   */
  data: O[];
  rowCount: number;
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
  initialGlobalFilter = "",
  initialSorting = [],
  filterEntries,
  favoriteId,
  favoriteName,
  buildFilter,
  enabled = true,
}: UseMagicFilterQueryOptions): UseMagicFilterQueryResult<O> {
  const [sorting, setSorting] = useState<SortingState>(initialSorting);
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize: initialPageSize,
  });
  const [globalFilter, setGlobalFilterState] = useState(initialGlobalFilter);

  const setGlobalFilter = (v: string) => {
    setGlobalFilterState(v);
    // Reset to first page on search change.
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

  const query = useQuery<ResultSet<O>>({
    queryKey: [...queryKey, filter],
    queryFn: ({ signal }) => fetchList<O>(entity, filter, signal),
    placeholderData: keepPreviousData,
    enabled,
  });

  const allRows = query.data?.resultSet;
  // Stable identity while no result is in: a fresh [] would rebuild the table's rows.
  const rows = useMemo(() => allRows ?? [], [allRows]);

  return {
    data: rows,
    rowCount: query.data?.totalSize ?? rows.length,
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
    setSorting(
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
