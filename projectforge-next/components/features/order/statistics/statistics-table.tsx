"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";

/** Above this many rows the table pages, so a long list of positions or invoices stays responsive. */
const PAGE_SIZE = 50;

/** The leading columns that identify a row (number, customer, project, …) and stay while scrolling. */
const PINNED_COLUMNS = 4;

/**
 * A data table of the order statistics: sorted and paged in the browser (the backend sends all rows at
 * once), the sums under their columns, with column lines as a table of figures.
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
}: {
  columns: ColumnDef<T, unknown>[];
  data: T[];
  getRowId: (row: T, index: number) => string;
  rowClassName?: (row: T) => string | undefined;
}) {
  const paged = data.length > PAGE_SIZE;
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
        columns={columns}
        data={data}
        enableColumnFilters={false}
        manualSorting={false}
        manualPagination={!paged}
        showPagination={paged}
        initialPageSize={PAGE_SIZE}
        columnPinning={columnPinning}
        columnLines
        getRowId={getRowId}
        rowClassName={rowClassName}
      />
    </div>
  );
}
