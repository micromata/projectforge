"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import type {
  OnChangeFn,
  PaginationState,
  SortingState,
} from "@tanstack/react-table";
import { fetchList } from "@/lib/rs/client";
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

  const filter: MagicFilter = useMemo(() => {
    const base: MagicFilter = {
      entries: [],
      sortProperties: sorting.map((s) => ({
        property: s.id,
        sortOrder: s.desc ? "DESCENDING" : "ASCENDING",
      })),
      searchString: globalFilter || undefined,
      paginationPageSize: pagination.pageSize,
      extended: { page: pagination.pageIndex },
    };
    return buildFilter ? buildFilter(base) : base;
  }, [sorting, pagination, globalFilter, buildFilter]);

  const query = useQuery<ResultSet<O>>({
    queryKey: [...queryKey, filter],
    queryFn: ({ signal }) => fetchList<O>(entity, filter, signal),
    placeholderData: keepPreviousData,
    enabled,
  });

  return {
    data: query.data?.resultSet ?? [],
    rowCount: query.data?.totalSize ?? 0,
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
