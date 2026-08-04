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
import type { MagicFilter, ResultSet } from "@/lib/rs/types";

interface UseMagicFilterQueryOptions {
  /** Backend entity, e.g. "book" — maps to POST /rs/{entity}/list. */
  entity: string;
  /** React Query key prefix. */
  queryKey: readonly unknown[];
  initialPageSize?: number;
  initialGlobalFilter?: string;
  initialSorting?: SortingState;
  /** Hook that lets callers customize the MagicFilter before it's sent. */
  buildFilter?: (base: MagicFilter) => MagicFilter;
  enabled?: boolean;
}

interface UseMagicFilterQueryResult<O> {
  data: O[];
  rowCount: number;
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
}

export function useMagicFilterQuery<O>({
  entity,
  queryKey,
  initialPageSize = 50,
  initialGlobalFilter = "",
  initialSorting = [],
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

  // The page index is deliberately not part of the filter: AbstractPagesRest.getList
  // returns the whole result list (capped by maxRows) rather than a single page, so
  // paging happens on the client below. Only the page size is sent, as the entry the
  // backend expects.
  const filter: MagicFilter = useMemo(() => {
    const base: MagicFilter = {
      entries: [paginationPageSizeEntry(pagination.pageSize)],
      sortProperties: sorting.map((s) => ({
        property: s.id,
        sortOrder: s.desc ? "DESCENDING" : "ASCENDING",
      })),
      searchString: globalFilter || undefined,
    };
    return buildFilter ? buildFilter(base) : base;
  }, [sorting, pagination.pageSize, globalFilter, buildFilter]);

  const query = useQuery<ResultSet<O>>({
    queryKey: [...queryKey, filter],
    queryFn: ({ signal }) => fetchList<O>(entity, filter, signal),
    placeholderData: keepPreviousData,
    enabled,
  });

  const allRows = query.data?.resultSet;
  const pageRows = useMemo(() => {
    const start = pagination.pageIndex * pagination.pageSize;
    return (allRows ?? []).slice(start, start + pagination.pageSize);
  }, [allRows, pagination.pageIndex, pagination.pageSize]);

  return {
    data: pageRows,
    rowCount: query.data?.totalSize ?? allRows?.length ?? 0,
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
  };
}
