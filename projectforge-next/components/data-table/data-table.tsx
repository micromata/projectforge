"use client";

import {
  flexRender,
  type Column,
  type Row,
  type Table as TanstackTable,
} from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { useDataTable, type UseDataTableOptions } from "./use-data-table";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { DataTablePagination } from "./data-table-pagination";

const ROW_ACTIONS_WIDTH = 80;

/**
 * Sticky offsets for pinned columns. Uses getStart/getAfter so several columns can
 * be pinned to the same side without overlapping.
 */
function pinnedStyle<TData>(column: Column<TData, unknown>): React.CSSProperties {
  const pinned = column.getIsPinned();
  if (!pinned) return {};
  return {
    position: "sticky",
    left: pinned === "left" ? column.getStart("left") : undefined,
    right: pinned === "right" ? column.getAfter("right") : undefined,
    // Above scrolling cells, below the sticky header (z-20).
    zIndex: 1,
  };
}

/** Marks the boundary between pinned and scrolling columns. */
function pinnedClass<TData>(
  column: Column<TData, unknown>,
  /** Header cells bring their own background; body cells need an opaque one so
   *  scrolling columns don't show through. */
  opaque = true
): string | undefined {
  const pinned = column.getIsPinned();
  if (!pinned) return undefined;
  return cn(
    opaque && "bg-background",
    pinned === "left" && column.getIsLastColumn("left") && "border-r",
    pinned === "right" && column.getIsFirstColumn("right") && "border-l"
  );
}

export interface DataTableProps<TData> extends UseDataTableOptions<TData> {
  /** Pass a table created by useDataTable to share it with a toolbar; otherwise
   *  DataTable creates its own. */
  table?: TanstackTable<TData>;

  isLoading?: boolean;
  isFetching?: boolean;

  onRowClick?: (row: TData) => void;
  rowActions?: (row: TData) => React.ReactNode;

  emptyState?: React.ReactNode;
  className?: string;
}

export function DataTable<TData>({
  table: tableProp,
  isLoading = false,
  isFetching = false,
  onRowClick,
  rowActions,
  emptyState,
  className,
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
        <Table
          className="min-w-full table-fixed text-xs [&_td]:px-2 [&_td]:py-1 [&_th]:h-7 [&_th]:px-2"
          style={{ width: totalWidth }}
        >
          <colgroup>
            {table.getVisibleLeafColumns().map((column) => (
              <col key={column.id} style={{ width: column.getSize() }} />
            ))}
            {rowActions && <col style={{ width: ROW_ACTIONS_WIDTH }} />}
            <col />
          </colgroup>
          <TableHeader className="sticky top-0 z-20 bg-muted">
            {table.getHeaderGroups().map((hg) => (
              <TableRow key={hg.id}>
                {hg.headers.map((header) => (
                  <TableHead
                    key={header.id}
                    style={pinnedStyle(header.column)}
                    className={cn(
                      // Own background: the sticky header would otherwise let
                      // rows show through as they scroll underneath.
                      "group/th relative truncate bg-muted text-[10px]",
                      header.column.getIsSorted() && "bg-primary/10",
                      pinnedClass(header.column, false)
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
                  />
                ))
            )}
          </TableBody>
        </Table>
      </div>
      <DataTablePagination table={table} />
    </div>
  );
}

interface DataTableRowProps<TData> {
  row: Row<TData>;
  onRowClick?: (row: TData) => void;
  rowActions?: (row: TData) => React.ReactNode;
}

function DataTableRow<TData>({
  row,
  onRowClick,
  rowActions,
}: DataTableRowProps<TData>) {
  return (
    <TableRow
      className={cn("group", onRowClick && "cursor-pointer")}
      onClick={onRowClick ? () => onRowClick(row.original) : undefined}
    >
      {row.getVisibleCells().map((cell, index) => (
        <TableCell
          key={cell.id}
          style={pinnedStyle(cell.column)}
          className={cn(
            "truncate",
            // Hover marker as a pseudo element on the first cell: a dedicated <td>
            // would occupy a column slot and shift every cell out of its column.
            index === 0 &&
              "relative before:pointer-events-none before:absolute before:inset-y-0 before:left-0 before:w-[3px] before:bg-primary before:opacity-0 before:transition-opacity group-hover:before:opacity-100",
            pinnedClass(cell.column)
          )}
        >
          {flexRender(cell.column.columnDef.cell, cell.getContext())}
        </TableCell>
      ))}
      {rowActions && (
        <TableCell>
          <div
            className="flex items-center justify-end gap-1 opacity-0 transition-opacity group-hover:opacity-100"
            onClick={(e) => e.stopPropagation()}
          >
            {rowActions(row.original)}
          </div>
        </TableCell>
      )}
      <TableCell aria-hidden />
    </TableRow>
  );
}
