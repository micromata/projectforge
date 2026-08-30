"use client";

import { useEffect, useRef } from "react";
import { flexRender, type Table as TanstackTable } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { useDataTable, type UseDataTableOptions } from "./use-data-table";
import {
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { useCollapseOnScroll } from "@/hooks/use-collapse-on-scroll";
import { DataTablePagination } from "./data-table-pagination";
import { DataTableRow, pinnedClass, pinnedStyle } from "./data-table-row";
import { TableLoadingOverlay } from "./table-loading-overlay";
import {
  getScrollParent,
  scrollRowIntoView,
  useHighlightedRow,
} from "./use-highlighted-row";
import type { KeyboardNav } from "./use-keyboard-nav";
import {
  recallMarkedRowId,
  rememberMarkedRow,
  useRememberScroll,
} from "./use-list-view-memory";
import { useOverflowTooltip } from "./use-overflow-tooltip";
import type { RowSelection } from "./use-row-selection";

const ROW_ACTIONS_WIDTH = 80;

export interface DataTableProps<TData> extends UseDataTableOptions<TData> {
  /** Pass a table created by useDataTable to share it with a toolbar; otherwise
   *  DataTable creates its own. */
  table?: TanstackTable<TData>;

  isLoading?: boolean;
  isFetching?: boolean;

  onRowClick?: (row: TData) => void;
  /**
   * Click on a single cell, told apart by its column.
   *
   * Exists because a row-level handler cannot express what the structure tree needs: a click on the
   * first column expands the node, a click on any other column selects it (which is what the hint
   * below that table says). Same click, same row, two meanings — only the column tells them apart.
   *
   * Fires instead of [onRowClick] where both are set: the cell handler stops the event.
   */
  onCellClick?: (row: TData, columnId: string) => void;
  rowActions?: (row: TData) => React.ReactNode;
  /**
   * Highlight class for a whole row, e.g. "row-red" — the semantic classes in
   * globals.css. Used by the dynamic grid to reproduce a list's row colours.
   */
  rowClassName?: (row: TData) => string | undefined;
  /**
   * The row the user edited last: marked, and brought into view once (see useHighlightedRow). Its
   * own prop rather than something a caller folds into [rowClassName], because the marker and the
   * row's colour are two layers, and because the scrolling belongs to the table's own container.
   */
  highlightRowId?: number | null;
  /**
   * What the highlighted id is counted per, e.g. the entity name — so two tables cannot mistake each
   * other's ids for one already scrolled to, and so the scroll happens once for the whole session
   * rather than on every visit to the list. Left out where a table should scroll again whenever it
   * mounts (see useHighlightedRow).
   */
  highlightScope?: string;
  /**
   * What the page, the scroll offset and the opened entry are remembered per, e.g. the entity name —
   * so leaving the list and coming back returns to where the user was, with the entry they looked at
   * marked (see useRememberScroll, recallPageIndex and recallMarkedRowId).
   *
   * Left out for a table that should always open at the top: a bounded one inside a form
   * (SelectedEntriesTable) or inside a dialog (the task picker), where there is no leaving and coming
   * back. Its own prop rather than a second meaning of [highlightScope], which counts ids.
   */
  viewScope?: string;

  /**
   * Rows can be picked for a mass update: a click selects instead of opening, the arrow keys move
   * through the rows and the checkbox column is shown (see useRowSelection, which produces this).
   *
   * Passed as a whole rather than as single handlers, because the table has to render what the same
   * state says — the tinted rows, the focused one — and half of it would be a table that answers
   * clicks without showing their effect.
   */
  selection?: RowSelection;

  /**
   * Drive the table from the keyboard without the multi-select mode: one focused row moved by the
   * arrow keys, the domain deciding what each key does to it (see KeyboardNav). Mutually exclusive
   * with [selection] in practice — a table is either picking rows or being walked through, not both.
   * The structure tree passes this so it reads like a file explorer (see useTreeKeyboard).
   */
  keyboardNav?: KeyboardNav;
  /**
   * Focus the keyboard body on mount so the arrow keys work without a click first. The initial
   * focused row is already seeded by the caller's [keyboardNav] (see useTreeKeyboard's initialFocusId),
   * so this only puts the DOM focus where the keys are handled. On by the task-select panel, where the
   * tree opens on the current task; off on the standalone tree page, which must not steal focus or
   * scroll on load — there a click focuses the body as before.
   */
  autoFocusKeyboard?: boolean;

  /** Page sizes the pagination select offers; defaults to PAGE_SIZE_OPTIONS. */
  pageSizeOptions?: number[];
  /**
   * Whether the pagination bar is shown at all.
   *
   * Off for a table that is a fixed, small set of rows the user cannot page through - the wizard's
   * preview of the rights it would set, which is as long as the path it walks. "1-4 of 4" and a page
   * size select would be chrome around four rows.
   */
  showPagination?: boolean;

  emptyState?: React.ReactNode;
  /** Rendered between the scrollable table area and the pagination bar (e.g. a colour legend). */
  footer?: React.ReactNode;
  className?: string;
  /**
   * Whether scrolling this table collapses the app's logo row (see hooks/use-collapse-on-scroll.ts).
   *
   * Off by default, and set only where the table *is* the page's scroll column: a bounded table inside
   * a form (SelectedEntriesTable) or inside a dialog (the task picker) must not move the app's header.
   */
  collapseLogoOnScroll?: boolean;
  /**
   * Tighter rows: half the vertical cell padding, for a table meant to show as many rows at once as it
   * can - the structure tree, whose file-explorer view is judged by how much of a deep tree fits on
   * screen (see Wicket's taskTree). Off by default, so an ordinary list keeps its comfortable spacing.
   */
  dense?: boolean;
  /**
   * Grow to the full height of the rows instead of keeping an inner vertical scroller: the table is
   * then scrolled by the page (`<main>`, see PageShell, which names the task tree as one of its scroll
   * columns), so the whole structure stands vertically complete with nothing folded behind a scrollbar.
   *
   * The sticky header and the pinned columns anchor to that ancestor instead of an inner box, and the
   * scroll helpers (see [containerRef]) act on it too. One consequence of scrolling the page in both
   * axes: a table wider than the viewport scrolls the page's title and filter row along with it — the
   * price of not having a second, inner scroll container that would trap the sticky header. Off by
   * default; a bounded table (a list page, a dialog) keeps its own scroller.
   */
  autoHeight?: boolean;
}

export function DataTable<TData>({
  table: tableProp,
  isLoading = false,
  isFetching = false,
  onRowClick,
  onCellClick,
  rowActions,
  rowClassName,
  highlightRowId,
  highlightScope,
  viewScope,
  selection,
  keyboardNav,
  autoFocusKeyboard = false,
  pageSizeOptions,
  showPagination = true,
  emptyState,
  footer,
  className,
  collapseLogoOnScroll = false,
  dense = false,
  autoHeight = false,
  ...tableOptions
}: DataTableProps<TData>) {
  const t = useTranslations("table");
  // Only used when no table was passed in; the hook must run unconditionally.
  const ownTable = useDataTable(tableOptions);
  const table = tableProp ?? ownTable;

  const visibleColumns = table.getVisibleLeafColumns();
  const cols = visibleColumns.length + (rowActions ? 1 : 0) + 1; // + filler
  // The table is exactly the sum of its column widths; the filler column then
  // stretches to the container so the header background spans the full width.
  const totalWidth =
    table.getTotalSize() + (rowActions ? ROW_ACTIONS_WIDTH : 0);
  const showSkeleton = isLoading && table.getRowModel().rows.length === 0;
  const overflowTooltip = useOverflowTooltip();
  const collapseLogo = useCollapseOnScroll(collapseLogoOnScroll);

  const scrollRef = useRef<HTMLDivElement | null>(null);
  // In autoHeight the inner div does not scroll — the page does — so the scroll helpers below have to
  // act on that ancestor, not on `scrollRef`. Found once from the mounted table (a settled layout);
  // null otherwise, so the ordinary bounded table keeps using its own container.
  const scrollParentRef = useRef<HTMLElement | null>(null);
  useEffect(() => {
    scrollParentRef.current = autoHeight
      ? getScrollParent(scrollRef.current)
      : null;
  }, [autoHeight]);
  const containerRef = autoHeight ? scrollParentRef : scrollRef;
  // Entering the selection mode puts the keyboard on the table: the arrow keys are handled by the body
  // below, so without this they do nothing until a click happens to focus it — which is what made
  // "↑/↓ only work after I marked a row" the way the mode used to behave.
  const bodyRef = useRef<HTMLTableSectionElement | null>(null);
  const focusFirstRow = selection?.focusFirstRow;
  useEffect(() => {
    if (!focusFirstRow) return;
    bodyRef.current?.focus({ preventScroll: true });
    focusFirstRow();
  }, [focusFirstRow]);
  // The same for keyboardNav where the caller asks for it (the select panel): its focused row is
  // already seeded, so this only puts the DOM focus on the body the keys are handled by, without a
  // click. preventScroll — the seeded row is brought into view by the keyboardFocusedRowId effect
  // below, so scrolling here too would jump twice.
  useEffect(() => {
    if (!keyboardNav || !autoFocusKeyboard) return;
    bodyRef.current?.focus({ preventScroll: true });
  }, [keyboardNav, autoFocusKeyboard]);
  // Keyboard navigation moves a focused row that the table has to keep on screen — but only when it
  // scrolls off, so arrowing between rows already visible doesn't jerk the viewport (see
  // scrollRowIntoView). The row must be in the document first, hence an effect and not the key handler.
  const keyboardFocusedRowId = keyboardNav?.focusedRowId;
  useEffect(() => {
    if (keyboardFocusedRowId == null) return;
    scrollRowIntoView(containerRef.current, keyboardFocusedRowId);
  }, [keyboardFocusedRowId, containerRef]);
  // Both work on the same container and the same settled rows, and the highlight has the last word —
  // hence the flag between them rather than two hooks scrolling independently.
  const highlightPending = useHighlightedRow({
    table,
    highlightRowId,
    containerRef,
    // Only on rows that are settled. A skeleton has no row to scroll to, and rows still being
    // fetched are the *previous* result set (`keepPreviousData`): jumping to a page of those is
    // undone a moment later, because TanStack resets the page index when the data is replaced.
    ready: !showSkeleton && !isFetching,
    scope: highlightScope,
  });
  const rememberScroll = useRememberScroll({
    containerRef,
    scope: viewScope,
    ready: !showSkeleton && !isFetching,
    highlightPending,
  });
  // The row to mark: the entry the user opened from here — browser-back then says which one that was,
  // the way returning from a save or a cancel does. The backend's id wins while its jump is still to
  // come, because that jump *is* a save or a cancel just gone by (see useRememberScroll, which forgets
  // the opened entry for the same reason).
  const markedRowId =
    (highlightPending ? undefined : recallMarkedRowId(viewScope)) ??
    (highlightRowId != null ? String(highlightRowId) : undefined);

  return (
    <div
      className={cn(
        "flex flex-col",
        // A bounded table clips to its box and lets the inner div below scroll; an autoHeight one grows
        // and hands the scrolling to the page (see [autoHeight]).
        !autoHeight && "flex-1 overflow-hidden",
        className
      )}
    >
      {/* The overlay's positioning parent, and not the scroll container below it: `inset-0` there
          would be the whole scrolled content, so the spinner would sit at the top of the rows and
          scroll out of sight instead of staying where the user is looking. */}
      <div
        className={cn(
          "relative flex flex-col",
          !autoHeight && "flex-1 overflow-hidden"
        )}
      >
        {/* Also over the skeleton, which is where the wait is longest: a first load of the order book
            takes seconds, and eight rows of grey bars say "there is a table here", not "it is being
            fetched" — least of all that it is still being fetched after the second one. */}
        {isFetching && <TableLoadingOverlay />}
        <div
          ref={scrollRef}
          className={cn(
            "relative bg-background",
            // The scroll container of a bounded table; in autoHeight the div just holds the table and
            // the page scrolls, so the sticky header anchors to the page rather than to this box.
            !autoHeight && "flex-1 overflow-auto"
          )}
          aria-busy={isFetching}
          {...overflowTooltip.handlers}
          // Three listeners on the one column: the tooltip clears itself, the collapse drives the logo
          // row, and the offset is recorded for the next visit. The spread has to come first, or it
          // would drop this composed handler.
          onScroll={(event) => {
            overflowTooltip.handlers.onScroll();
            collapseLogo.onScroll(event);
            rememberScroll();
          }}
        >
          {/* table-fixed makes the colgroup widths authoritative — without it the
              browser sizes columns by content and header and body drift apart.
              Every width is explicit and the table is exactly as wide as their sum,
              so resizing one column leaves the others alone: any spare space in the
              container goes to the filler column below, never to the data columns. */}
          {/* Plain <table> instead of the shadcn Table primitive: that one wraps the
              table in its own overflow-x-auto element, which becomes the scroll
              container the sticky header would stick to — the wrong one, since
              vertical scrolling happens further out. */}
          <table
            className={cn(
              "min-w-full table-fixed border-separate border-spacing-0 text-xs [&_td]:px-2 [&_th]:h-7 [&_th]:px-2",
              dense ? "[&_td]:py-0.5" : "[&_td]:py-1"
            )}
            style={{ width: totalWidth }}
          >
            <colgroup>
              {table.getVisibleLeafColumns().map((column) => (
                <col key={column.id} style={{ width: column.getSize() }} />
              ))}
              {rowActions && <col style={{ width: ROW_ACTIONS_WIDTH }} />}
              <col />
            </colgroup>
            <TableHeader>
              {table.getHeaderGroups().map((hg) => (
                <TableRow key={hg.id}>
                  {hg.headers.map((header) => (
                    <TableHead
                      key={header.id}
                      style={pinnedStyle(header.column, true)}
                      // The whole cell sorts, rather than a button around the label:
                      // such a button competes with the filter icon for space and
                      // pushes it out of a narrow column. Shift-click adds a column
                      // to the sort (TanStack's default).
                      onClick={header.column.getToggleSortingHandler()}
                      className={cn(
                        // sticky per cell (not on thead): with border-collapse
                        // sticky is ignored on thead/tr. Own opaque background so
                        // rows don't show through while scrolling underneath — the
                        // sorted tint goes on a layer above it (see below), since a
                        // translucent tint alone would let rows through.
                        "group/th sticky top-0 z-20 truncate border-b bg-muted text-[10px]",
                        // select-none: shift-clicking would otherwise select text.
                        header.column.getCanSort() &&
                          "cursor-pointer select-none",
                        header.column.getIsSorted() &&
                          "before:pointer-events-none before:absolute before:inset-0 before:bg-primary/10",
                        pinnedClass(header.column)
                      )}
                    >
                      {header.isPlaceholder
                        ? null
                        : flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                      {header.column.getCanResize() && (
                        <span
                          role="separator"
                          aria-orientation="vertical"
                          aria-label={t("resize")}
                          onMouseDown={header.getResizeHandler()}
                          onTouchStart={header.getResizeHandler()}
                          onClick={(e) => e.stopPropagation()}
                          className={cn(
                            "absolute inset-y-0 right-0 w-1 cursor-col-resize touch-none select-none",
                            "bg-border transition-colors hover:bg-primary",
                            header.column.getIsResizing() && "bg-primary"
                          )}
                        />
                      )}
                    </TableHead>
                  ))}
                  {rowActions && <TableHead />}
                  {/* Filler: absorbs leftover container width so resizing a column
                      never redistributes width across the others. */}
                  <TableHead aria-hidden />
                </TableRow>
              ))}
            </TableHeader>
            {/* Focusable while rows can be picked, so the arrow keys reach the table at all — focused
                by the effect above the moment the mode is entered, and by a click on a row after that.
                `outline-none`: the focus ring would frame the whole body, while what the user follows
                is the marked row (`row-focused`). */}
            <TableBody
              ref={bodyRef}
              className={selection || keyboardNav ? "outline-none" : undefined}
              tabIndex={selection || keyboardNav ? 0 : undefined}
              onKeyDown={selection?.onKeyDown ?? keyboardNav?.onKeyDown}
              // Clicking into the body focuses it (it is `tabIndex=0`); this also moves the keyboard
              // focus onto the clicked row, so the arrow keys continue from where the user pointed
              // rather than from the top. The row id is read off the nearest `[data-row-id]`, so no
              // per-row handler is needed.
              onMouseDown={
                keyboardNav
                  ? (event) => {
                      const rowId = (event.target as HTMLElement)
                        .closest("[data-row-id]")
                        ?.getAttribute("data-row-id");
                      if (rowId) keyboardNav.focusRow(rowId);
                    }
                  : undefined
              }
              aria-multiselectable={selection ? true : undefined}
            >
              {showSkeleton ? (
                Array.from({ length: 8 }).map((_, i) => (
                  <TableRow key={`skeleton-${i}`}>
                    {table.getVisibleLeafColumns().map((c) => (
                      <TableCell key={c.id}>
                        <Skeleton className="h-4 w-full max-w-32" />
                      </TableCell>
                    ))}
                    {rowActions && <TableCell />}
                    <TableCell aria-hidden />
                  </TableRow>
                ))
              ) : table.getRowModel().rows.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={cols}
                    className="py-12 text-center text-sm text-muted-foreground"
                  >
                    {emptyState ?? t("empty")}
                  </TableCell>
                </TableRow>
              ) : (
                table.getRowModel().rows.map((row) => (
                  <DataTableRow
                    key={row.id}
                    row={row}
                    onRowClick={
                      onRowClick &&
                      ((original: TData) => {
                        // Noted here rather than in the caller's handler: the id is the table's own,
                        // so every list that remembers its view marks the opened entry for free.
                        rememberMarkedRow(viewScope, row.id);
                        onRowClick(original);
                      })
                    }
                    onCellClick={onCellClick}
                    onSelectClick={selection?.onRowClick}
                    rowActions={rowActions}
                    // The row's colour and the marker are two layers, so both classes apply — see
                    // `row-highlighted` in globals.css, which is why it is no background.
                    className={cn(
                      rowClassName?.(row.original),
                      row.id === markedRowId && "row-highlighted",
                      selection && row.getIsSelected() && "row-selected",
                      (selection?.focusedRowId ?? keyboardNav?.focusedRowId) ===
                        row.id && "row-focused"
                    )}
                  />
                ))
              )}
            </TableBody>
          </table>
          {overflowTooltip.tooltip}
        </div>
      </div>
      {footer}
      {showPagination && (
        <DataTablePagination table={table} pageSizeOptions={pageSizeOptions} />
      )}
    </div>
  );
}
