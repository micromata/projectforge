"use client";

import { useEffect, useRef } from "react";
import type { RefObject } from "react";
import type { Table } from "@tanstack/react-table";

/**
 * Height of the sticky header, which covers the top of the scrolled content (`[&_th]:h-7` in
 * DataTable). A row brought to the very top of the container would sit underneath it.
 */
const STICKY_HEADER_HEIGHT = 28;

/** A little air above the row, so it doesn't look glued to the header. */
const SCROLL_MARGIN = 8;

/**
 * The nearest ancestor that scrolls, so the scroll helpers can act on it.
 *
 * A table with its own inner scroll container is its own scroller; one grown to full height (see
 * DataTable's `autoHeight`) is scrolled by the page instead — `<main>` in this app (see PageShell,
 * which names the task tree as one of its scroll columns). The measurements below are all rect
 * arithmetic, so they work on either.
 */
export function getScrollParent(element: HTMLElement | null): HTMLElement | null {
  let node = element?.parentElement ?? null;
  while (node) {
    const overflowY = getComputedStyle(node).overflowY;
    if (overflowY === "auto" || overflowY === "scroll") return node;
    node = node.parentElement;
  }
  return null;
}

/**
 * Ids already scrolled to, keyed by scope so two entities cannot mistake each other's ids.
 *
 * Module level rather than a ref: the list unmounts on every navigation, so a ref would forget and
 * the table would yank the viewport around on each visit — while the backend keeps handing the id
 * back for the rest of the session (the pref is never cleared). What survives a reload of the page
 * is nothing, which is wanted: after F5 the user is taken to their entry once more.
 */
const scrolledTo = new Set<string>();

/**
 * Brings a row into view only when it is not fully visible — the scroll a keyboard navigation needs as
 * the focus moves down or up (see DataTable's keyboardNav). Unlike [useHighlightedRow], which pages to
 * the row and lifts it near the top once, this leaves the viewport alone while the focused row stays on
 * screen, so arrowing through visible rows doesn't jerk the list.
 *
 * The measurement is the same rect arithmetic [useHighlightedRow] uses: `offsetTop` counts from the
 * nearest positioned ancestor, which need not be the scroll container.
 */
export function scrollRowIntoView(
  container: HTMLElement | null,
  rowId: string
): void {
  const row = container?.querySelector<HTMLElement>(
    `[data-row-id="${CSS.escape(rowId)}"]`
  );
  if (!container || !row) return;
  const rowTop =
    row.getBoundingClientRect().top -
    container.getBoundingClientRect().top +
    container.scrollTop;
  const rowBottom = rowTop + row.offsetHeight;
  // The sticky header covers the top of the scrolled content, so "visible" starts below it.
  const viewTop = container.scrollTop + STICKY_HEADER_HEIGHT;
  const viewBottom = container.scrollTop + container.clientHeight;
  if (rowTop < viewTop) {
    container.scrollTo({
      top: Math.max(0, rowTop - STICKY_HEADER_HEIGHT - SCROLL_MARGIN),
      behavior: "smooth",
    });
  } else if (rowBottom > viewBottom) {
    container.scrollTo({
      top: rowBottom - container.clientHeight + SCROLL_MARGIN,
      behavior: "smooth",
    });
  }
}

interface UseHighlightedRowOptions<TData> {
  table: Table<TData>;
  /** The row to mark, as the backend remembers it (see ResultSet.highlightRowId). */
  highlightRowId?: number | null;
  /** The table's scroll container. */
  containerRef: RefObject<HTMLElement | null>;
  /** Whether the rows on screen are the real ones — no skeleton, no stale page. */
  ready: boolean;
  /**
   * Tells one table's ids from another's, e.g. the entity name. Left out for a table that should
   * scroll again on every mount: the tree in the task-select dialog is reopened *in order* to look
   * at the selected task, unlike a list page, which is returned to for all sorts of reasons.
   */
  scope?: string;
}

/**
 * Brings the row the user edited last into view, once.
 *
 * Two steps, because paging happens on the client over the whole result set: the row is usually not on
 * the page the list opens on — not even where that page is the one the list was left on (see
 * recallPageIndex), since the entry may have been created or renamed into a different place. So the
 * table is paged to it first, and the scroll happens on the pass after that — the row has to exist in
 * the document before it can be measured.
 *
 * Marking the row is not this hook's job; DataTable adds the class (see `row-highlighted` in
 * globals.css), which is why the highlight stays for as long as the backend reports the id while
 * the scrolling here happens exactly once — and why the same marker can come from the entry the user
 * merely opened, which the backend knows nothing about (see recallMarkedRowId).
 *
 * @returns whether that jump is still to come — what tells the remembered offset to stand down, since
 *   a user returning from an edit is looking for their entry (see useRememberScroll).
 */
export function useHighlightedRow<TData>({
  table,
  highlightRowId,
  containerRef,
  ready,
  scope,
}: UseHighlightedRowOptions<TData>): boolean {
  // Reading the row model in the dependency array is what makes this run again after the page jump
  // below, and after a refetch replaced the rows.
  const rows = table.getRowModel().rows;
  /** Without a scope: done for this mount only, so the next one scrolls again. */
  const done = useRef<number | null>(null);

  useEffect(() => {
    if (!ready || highlightRowId == null) return;
    const key = scope ? `${scope}:${highlightRowId}` : undefined;
    if (key ? scrolledTo.has(key) : done.current === highlightRowId) return;

    // Sorted, not paged: the index within the whole result set is what names the page. With
    // `manualSorting` this is the order the server sent, which is the same thing.
    const index = table
      .getSortedRowModel()
      .rows.findIndex((row) => row.id === String(highlightRowId));
    // Not in the list at all — a column filter may hide it, or it was deleted for good. Deliberately
    // not remembered as done: should the filter go, the row is still worth showing.
    if (index < 0) return;

    const { pageIndex, pageSize } = table.getState().pagination;
    const targetPage = Math.floor(index / pageSize);
    if (targetPage !== pageIndex) {
      table.setPageIndex(targetPage);
      // The rows of that page render first; this effect runs again on them and scrolls.
      return;
    }

    const container = containerRef.current;
    const row = container?.querySelector<HTMLElement>(
      `[data-row-id="${CSS.escape(String(highlightRowId))}"]`
    );
    if (!container || !row) return;

    // Measured through the rects rather than `offsetTop`, as useScrollSpy does: that one counts from
    // the nearest positioned ancestor, which need not be the scroll container.
    const top =
      row.getBoundingClientRect().top -
      container.getBoundingClientRect().top +
      container.scrollTop -
      STICKY_HEADER_HEIGHT -
      SCROLL_MARGIN;
    container.scrollTo({ top: Math.max(0, top), behavior: "smooth" });
    if (key) scrolledTo.add(key);
    else done.current = highlightRowId;
  }, [table, highlightRowId, containerRef, ready, scope, rows]);

  // Read during render, so it says what this pass knows — the effect marks the row as done without a
  // re-render, and the caller reads this once, on the pass where the rows have settled. Only the
  // scoped bookkeeping: the `done` ref above is not a render-time value, and a table without a scope
  // is one that scrolls on every mount anyway, so it has no remembered offset to stand down for.
  const scopeKey =
    scope && highlightRowId != null ? `${scope}:${highlightRowId}` : undefined;
  return scopeKey != null && !scrolledTo.has(scopeKey);
}
