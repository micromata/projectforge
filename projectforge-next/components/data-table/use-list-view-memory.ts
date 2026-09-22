"use client";

import { useEffect, useRef } from "react";
import type { RefObject } from "react";
import type { Table } from "@tanstack/react-table";

/** Where a list was left: which page, how far down it was scrolled, and on which entry. */
interface ListView {
  pageIndex: number;
  scrollTop: number;
  /** The row whose entry the user opened from here, as the table's own id (see getRowId). */
  markedRowId?: string;
}

/**
 * Per scope, e.g. the entity name — so two lists cannot restore each other's page.
 *
 * Module level, like the `scrolledTo` set in useHighlightedRow, and for the same reason: the list
 * unmounts on every navigation, so anything held in a ref or in state would be forgotten exactly
 * when it is needed. What does *not* survive is a reload, which is wanted — F5 starts a list afresh,
 * and it keeps this out of web storage, which the app uses nowhere.
 *
 * A stale entry can only mean the list opens on the wrong page: the rows themselves come from the
 * query, and the clamp below catches an index the result set no longer has.
 */
const views = new Map<string, ListView>();

/** Changes one part of what is remembered, leaving the rest of the record alone. */
function update(scope: string, change: Partial<ListView>) {
  const view = views.get(scope) ?? { pageIndex: 0, scrollTop: 0 };
  views.set(scope, { ...view, ...change });
}

/**
 * The page the list was left on — the seed of the query's pagination state, hence a plain function
 * rather than a hook: nothing renders from it, it is read once in a state initializer.
 */
export function recallPageIndex(scope?: string): number {
  return (scope ? views.get(scope)?.pageIndex : undefined) ?? 0;
}

/** Notes the entry the user opens, so the list can mark it when they come back. */
export function rememberMarkedRow(scope: string | undefined, rowId: string) {
  if (scope) update(scope, { markedRowId: rowId });
}

/**
 * The entry the user last opened from this list, to be marked as the edited one is (see
 * `row-highlighted` in globals.css).
 *
 * Only a marker, and deliberately no scroll: the offset the list was left with already puts that row
 * back on screen, and a second scroll would only compete with it.
 */
export function recallMarkedRowId(scope?: string): string | undefined {
  return scope ? views.get(scope)?.markedRowId : undefined;
}

/**
 * Drops that note, because something more recent has happened to the entry — see the highlight in
 * useRememberScroll, which is what a save or a cancel leaves behind.
 */
export function forgetMarkedRow(scope?: string) {
  if (scope) update(scope, { markedRowId: undefined });
}

/**
 * Keeps the remembered page in step with the table's, and brings an index the result set no longer
 * has back into range.
 *
 * The clamp is this hook's and not TanStack's: `setPageIndex` clamps, a state seeded with
 * `recallPageIndex` does not, and `autoResetPageIndex` is off (see useDataTable) — so a list whose
 * result set shrank while the user was away would render an empty table.
 *
 * @param settled Whether the rows on screen are the real ones. Under `keepPreviousData` a fetch in
 *   flight still reports the previous set's page count, which is not the one to clamp against.
 */
export function useRememberPageIndex<TData>(
  scope: string | undefined,
  table: Table<TData>,
  pageIndex: number,
  settled: boolean
) {
  useEffect(() => {
    if (!scope) return;
    const view = views.get(scope);
    // Only on a real change: the first pass sees the index it was just seeded with, and writing then
    // would drop the scroll offset belonging to it. A page flip does drop it — the offset is the
    // page's, not the list's.
    if (view?.pageIndex !== pageIndex)
      update(scope, { pageIndex, scrollTop: 0 });
  }, [scope, pageIndex]);

  useEffect(() => {
    if (!settled) return;
    const pageCount = table.getPageCount();
    if (pageCount > 0 && pageIndex >= pageCount)
      table.setPageIndex(pageCount - 1);
  }, [table, pageIndex, settled]);
}

interface UseRememberScrollOptions {
  /** The table's scroll container. */
  containerRef: RefObject<HTMLElement | null>;
  /** What the offset is remembered per, e.g. the entity name. Absent: nothing is remembered. */
  scope?: string;
  /** Whether the rows on screen are the real ones — there is nothing to scroll before that. */
  ready: boolean;
  /**
   * Whether useHighlightedRow is still to bring the last edited row into view. That one wins: a user
   * coming back from an edit is looking for their entry, not for the offset they left.
   */
  highlightPending: boolean;
}

/**
 * Restores the scroll offset the list was left with, once per mount, and records it as the user
 * scrolls.
 *
 * @returns the scroll handler the container has to call.
 */
export function useRememberScroll({
  containerRef,
  scope,
  ready,
  highlightPending,
}: UseRememberScrollOptions) {
  /** Done for this mount — restoring twice would fight the user's own scrolling. */
  const restored = useRef(false);

  useEffect(() => {
    if (!scope || restored.current || !ready) return;
    restored.current = true;
    // Marked as done either way, so the highlight's jump is not undone a moment after it happened.
    if (highlightPending) {
      // A pending highlight is a save or a cancel just gone by, which is a newer thing to have
      // happened to this list than the entry it was last left on — so that entry stops being marked.
      forgetMarkedRow(scope);
      return;
    }
    const top = views.get(scope)?.scrollTop ?? 0;
    if (top > 0) containerRef.current?.scrollTo({ top });
  }, [scope, ready, highlightPending, containerRef]);

  /**
   * Recorded while scrolling rather than in an unmount cleanup: React invokes effects twice in
   * development, and the throwaway mount's cleanup would record a `scrollTop` of 0 — wiping the
   * offset before the mount that needs it reads it.
   *
   * Written on the spot rather than coalesced into a `requestAnimationFrame`: the row a user clicks
   * is usually the one they just scrolled to, and the frame carrying that last offset would be
   * cancelled by the very unmount it was recorded for — leaving whatever the scroll passed through
   * earlier. The write itself is a Map entry, which costs less than scheduling the frame would.
   */
  return function onScroll() {
    if (!scope) return;
    update(scope, { scrollTop: containerRef.current?.scrollTop ?? 0 });
  };
}
