"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable, DataTableColumnHeader } from "@/components/data-table";

/** Above this many rows the table pages, so a long list of positions or invoices stays responsive. */
const PAGE_SIZE = 200;

/** The leading columns that identify a row (number, customer, project, …) and stay while scrolling. */
const PINNED_COLUMNS = 4;

/**
 * A data table of the order statistics: sorted and paged in the browser (the backend sends all rows at
 * once), the sums under their columns, with column lines as a table of figures.
 *
 * Every column has a filter in its header: the kind its builder names (see statistics-table-columns), else
 * number for a right-aligned column and text otherwise. The sums follow the filtered rows.
 *
 * The table scrolls in its own box, bounded to the viewport, rather than the page: so the header, the
 * sums and the first columns stay in sight while the wide month columns are scrolled, like the frozen
 * panes of the Excel sheets.
 */
export function StatisticsTable<T>({
  columns,
  data,
  getRowId,
  rowClassName,
  highlightRowId,
  onRowClick,
}: {
  columns: ColumnDef<T, unknown>[];
  data: T[];
  getRowId: (row: T, index: number) => string;
  rowClassName?: (row: T) => string | undefined;
  /** A row to mark and bring into view on mount, paging to it (see DataTable). */
  highlightRowId?: number | null;
  /** E.g. opening the rows behind the clicked one in another table. */
  onRowClick?: (row: T) => void;
}) {
  const paged = data.length > PAGE_SIZE;
  const filterable = useMemo(
    () =>
      columns.map(
        (column) =>
          ({
            ...column,
            header: ({ column: col, table }) => (
              <DataTableColumnHeader
                column={col}
                table={table}
                filterKind={
                  column.meta?.filterKind ??
                  (column.meta?.align === "right" ? "number" : "text")
                }
              >
                {column.meta?.label ??
                  (typeof column.header === "string" ? column.header : col.id)}
              </DataTableColumnHeader>
            ),
          }) as ColumnDef<T, unknown>
      ),
    [columns]
  );
  const columnPinning = useMemo(
    () => ({
      left: columns
        .slice(0, PINNED_COLUMNS)
        .map((column) => column.id)
        .filter((id): id is string => id != null),
    }),
    [columns]
  );
  return (
    <div className="flex max-h-[70vh] flex-col">
      <DataTable<T>
        columns={filterable}
        data={data}
        enableColumnFilters
        manualSorting={false}
        manualPagination={!paged}
        showPagination={paged}
        initialPageSize={PAGE_SIZE}
        columnPinning={columnPinning}
        columnLines
        getRowId={getRowId}
        rowClassName={rowClassName}
        highlightRowId={highlightRowId}
        onRowClick={onRowClick}
      />
    </div>
  );
}
