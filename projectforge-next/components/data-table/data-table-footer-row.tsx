"use client";

import { flexRender, type Table } from "@tanstack/react-table";
import { TableCell, TableFooter, TableRow } from "@/components/ui/table";
import { cn } from "@/lib/utils";
import { pinnedClass, pinnedStyle } from "./data-table-row";

/**
 * A sums row below the rows, made of the columns' `footer` definitions, so each sum stands under its own
 * column (e.g. the totals of the contribution margin per project). Rendered by DataTable only if a visible
 * column defines a footer; columns without one get an empty cell.
 *
 * Sticks to the bottom like the header sticks to the top, with its own opaque background so rows don't
 * show through while scrolling underneath.
 */
export function DataTableFooterRow<TData>({
  table,
  hasRowActions,
  suspendPinning,
  columnLines,
}: {
  table: Table<TData>;
  hasRowActions: boolean;
  suspendPinning: boolean;
  columnLines: boolean;
}) {
  return (
    <TableFooter className="border-t-0 bg-transparent">
      {table.getFooterGroups().map((group) => (
        <TableRow key={group.id} className="hover:bg-transparent">
          {group.headers.map((header) => (
            <TableCell
              key={header.id}
              // A pinned sum stays above the scrolling ones: the body cells' z-index of pinnedStyle would
              // undercut the z-10 every footer cell sticks with, so the scrolled sums painted over it.
              style={{
                ...pinnedStyle(header.column, false, suspendPinning),
                ...(header.column.getIsPinned() &&
                  !suspendPinning && { zIndex: 20 }),
              }}
              className={cn(
                "sticky bottom-0 z-10 truncate border-t bg-muted font-semibold",
                header.column.columnDef.meta?.align === "right" && "text-right",
                columnLines && "border-r",
                pinnedClass(header.column, suspendPinning)
              )}
            >
              {header.isPlaceholder
                ? null
                : flexRender(
                    header.column.columnDef.footer,
                    header.getContext()
                  )}
            </TableCell>
          ))}
          {hasRowActions && <TableCell className="border-t bg-muted" />}
          <TableCell aria-hidden className="border-t bg-muted" />
        </TableRow>
      ))}
    </TableFooter>
  );
}
