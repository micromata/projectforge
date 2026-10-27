import type { ColumnDef } from "@tanstack/react-table";
import type { ReactNode } from "react";

/** A plain, unsortable, unfiltered column of the small tables of the wizard. */
export function column<T>(
  id: string,
  label: string,
  cell: (row: T) => ReactNode,
  size: number,
  align?: "right"
): ColumnDef<T, unknown> {
  return {
    id,
    header: label,
    size,
    enableSorting: false,
    meta: { label, align },
    cell: ({ row }) => cell(row.original),
  };
}
