import { useState } from "react";

/**
 * The page sizes every list offers, mirroring the backend's
 * LayoutListFilterUtils.PAGINATION_PAGE_SIZES (exposed per grid as
 * UIAgGrid.paginationPageSizeSelector).
 */
export const PAGE_SIZE_OPTIONS = [25, 50, 100, 200, 500, 1000];

/** Page size of a list the user has never changed the size on. */
export const DEFAULT_PAGE_SIZE = 50;

/**
 * The extra-large page size the pagination select offers after PAGE_SIZE_OPTIONS, for looking at a big
 * result on one page. Deliberately not "all rows": the server's cap (QueryFilter.QUERY_FILTER_MAX_ROWS,
 * 100 000) took ~20 s to serve for the unfiltered accounting records, and the table renders every row of
 * the page. A larger result simply gets a second page.
 *
 * Transient by design: it is never stored as the user's page size (see storablePageSize), so the next
 * visit opens with the size chosen before it.
 */
export const TRANSIENT_PAGE_SIZE = 10_000;

/** The page size to store (and to seed a list from): the transient size or nothing falls back to [fallback]. */
export function storablePageSize(
  size: number | undefined,
  fallback: number = DEFAULT_PAGE_SIZE
): number {
  return size === undefined || size === TRANSIENT_PAGE_SIZE ? fallback : size;
}

/**
 * The last storable page size the list was shown with, [initial] until the user picks one: what goes into
 * the stored column state and the filter while TRANSIENT_PAGE_SIZE is selected.
 */
export function useStorablePageSize(
  pageSize: number,
  initial: number | undefined
): number {
  const [last, setLast] = useState(() => storablePageSize(initial));
  // Adjusted during render (React's "storing information from previous renders"), not in an effect, so
  // the size stored never lags a render behind the one shown.
  if (pageSize !== TRANSIENT_PAGE_SIZE && pageSize !== last) setLast(pageSize);
  return pageSize === TRANSIENT_PAGE_SIZE ? last : pageSize;
}
