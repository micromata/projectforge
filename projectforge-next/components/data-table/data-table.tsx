"use client";

import {
  flexRender,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  type ColumnDef,
  type OnChangeFn,
  type PaginationState,
  type Row,
  type SortingState,
  type Table as TanstackTable,
  useReactTable,
} from "@tanstack/react-table";
import { useState } from "react";
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

export interface DataTableProps<TData> {
  columns: ColumnDef<TData, unknown>[];
  data: TData[];
  /** Total row count (server-side). Required when manualPagination is true. */
  rowCount?: number;

  // Controlled state (optional). If not provided, the component manages it internally.
  sorting?: SortingState;
  onSortingChange?: OnChangeFn<SortingState>;
  pagination?: PaginationState;
  onPaginationChange?: OnChangeFn<PaginationState>;

  manualSorting?: boolean;
  manualPagination?: boolean;
  manualFiltering?: boolean;

  isLoading?: boolean;
  isFetching?: boolean;

  onRowClick?: (row: TData) => void;
  rowActions?: (row: TData) => React.ReactNode;
  getRowId?: (row: TData, index: number) => string;

  emptyState?: React.ReactNode;
  className?: string;

  /** Optional render prop to access the TanStack table instance from outside. */
  tableRef?: (table: TanstackTable<TData>) => void;

  initialPageSize?: number;
}

export function DataTable<TData>({
  columns,
  data,
  rowCount,
  sorting: sortingProp,
  onSortingChange,
  pagination: paginationProp,
  onPaginationChange,
  manualSorting = false,
  manualPagination = false,
  manualFiltering = false,
  isLoading = false,
  isFetching = false,
  onRowClick,
  rowActions,
  getRowId,
  emptyState,
  className,
  tableRef,
  initialPageSize = 50,
}: DataTableProps<TData>) {
  const [internalSorting, setInternalSorting] = useState<SortingState>([]);
  const [internalPagination, setInternalPagination] = useState<PaginationState>(
    {
      pageIndex: 0,
      pageSize: initialPageSize,
    }
  );

  const sortingControlled = sortingProp !== undefined;
  const paginationControlled = paginationProp !== undefined;

  const table = useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: manualSorting ? undefined : getSortedRowModel(),
    getPaginationRowModel: manualPagination
      ? undefined
      : getPaginationRowModel(),
    manualSorting,
    manualPagination,
    manualFiltering,
    rowCount: manualPagination ? rowCount : undefined,
    state: {
      sorting: sortingControlled ? sortingProp : internalSorting,
      pagination: paginationControlled ? paginationProp : internalPagination,
    },
    onSortingChange: sortingControlled ? onSortingChange : setInternalSorting,
    onPaginationChange: paginationControlled
      ? onPaginationChange
      : setInternalPagination,
    getRowId,
  });

  if (tableRef) tableRef(table);

  const cols = table.getVisibleFlatColumns().length + (rowActions ? 1 : 0);
  const showSkeleton = isLoading && data.length === 0;

  return (
    <div className={cn("flex flex-1 flex-col overflow-hidden", className)}>
      <div className="relative flex-1 overflow-auto bg-background">
        {isFetching && !showSkeleton && (
          <div className="pointer-events-none absolute inset-x-0 top-0 z-10 h-0.5 animate-pulse bg-primary/40" />
        )}
        <Table className="text-sm">
          <TableHeader className="bg-muted/40">
            {table.getHeaderGroups().map((hg) => (
              <TableRow key={hg.id}>
                {hg.headers.map((header) => (
                  <TableHead
                    key={header.id}
                    style={{
                      width: header.column.columnDef.size,
                      minWidth: header.column.columnDef.minSize,
                      maxWidth: header.column.columnDef.maxSize,
                    }}
                    className={cn(
                      header.column.getIsSorted() && "bg-primary/10"
                    )}
                  >
                    {header.isPlaceholder
                      ? null
                      : flexRender(
                          header.column.columnDef.header,
                          header.getContext()
                        )}
                  </TableHead>
                ))}
                {rowActions && <TableHead style={{ width: 96 }} />}
              </TableRow>
            ))}
          </TableHeader>
          <TableBody>
            {showSkeleton ? (
              Array.from({ length: 8 }).map((_, i) => (
                <TableRow key={`skeleton-${i}`}>
                  {table.getVisibleFlatColumns().map((c) => (
                    <TableCell key={c.id}>
                      <Skeleton className="h-4 w-full max-w-32" />
                    </TableCell>
                  ))}
                  {rowActions && <TableCell />}
                </TableRow>
              ))
            ) : table.getRowModel().rows.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={cols}
                  className="py-12 text-center text-sm text-muted-foreground"
                >
                  {emptyState ?? "Keine Einträge"}
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
      className={cn("group relative", onRowClick && "cursor-pointer")}
      onClick={onRowClick ? () => onRowClick(row.original) : undefined}
    >
      <td
        aria-hidden
        className="pointer-events-none absolute inset-y-0 left-0 w-[3px] bg-primary opacity-0 transition-opacity group-hover:opacity-100"
      />
      {row.getVisibleCells().map((cell) => (
        <TableCell key={cell.id}>
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
    </TableRow>
  );
}
