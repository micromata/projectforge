"use client";

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
import { DataTablePagination } from "./data-table-pagination";
import { DataTableRow, pinnedClass, pinnedStyle } from "./data-table-row";

const ROW_ACTIONS_WIDTH = 80;

export interface DataTableProps<TData> extends UseDataTableOptions<TData> {
  /** Pass a table created by useDataTable to share it with a toolbar; otherwise
   *  DataTable creates its own. */
  table?: TanstackTable<TData>;

  isLoading?: boolean;
  isFetching?: boolean;

  onRowClick?: (row: TData) => void;
  rowActions?: (row: TData) => React.ReactNode;
  /**
   * Highlight class for a whole row, e.g. "row-red" — the semantic classes in
   * globals.css. Used by the dynamic grid to reproduce a list's row colours.
   */
  rowClassName?: (row: TData) => string | undefined;

  emptyState?: React.ReactNode;
  className?: string;
}

export function DataTable<TData>({
  table: tableProp,
  isLoading = false,
  isFetching = false,
  onRowClick,
  rowActions,
  rowClassName,
  emptyState,
  className,
  ...tableOptions
}: DataTableProps<TData>) {
  const t = useTranslations("table");
  const tColumns = useTranslations("columns");
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

  return (
    <div className={cn("flex flex-1 flex-col overflow-hidden", className)}>
      <div className="relative flex-1 overflow-auto bg-background">
        {isFetching && !showSkeleton && (
          <div className="pointer-events-none absolute inset-x-0 top-0 z-10 h-0.5 animate-pulse bg-primary/40" />
        )}
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
          className="min-w-full table-fixed border-separate border-spacing-0 text-xs [&_td]:px-2 [&_td]:py-1 [&_th]:h-7 [&_th]:px-2"
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
                    title={
                      header.column.getCanSort() ? tColumns("sort") : undefined
                    }
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
          <TableBody>
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
              table
                .getRowModel()
                .rows.map((row) => (
                  <DataTableRow
                    key={row.id}
                    row={row}
                    onRowClick={onRowClick}
                    rowActions={rowActions}
                    className={rowClassName?.(row.original)}
                  />
                ))
            )}
          </TableBody>
        </table>
      </div>
      <DataTablePagination table={table} />
    </div>
  );
}
