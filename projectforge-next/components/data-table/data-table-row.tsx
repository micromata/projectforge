"use client";

import { flexRender, type Column, type Row } from "@tanstack/react-table";
import { TableCell, TableRow } from "@/components/ui/table";
import { cn } from "@/lib/utils";

/**
 * Sticky offsets for pinned columns. Uses getStart/getAfter so several columns can
 * be pinned to the same side without overlapping.
 *
 * A pinned header cell sticks on both axes, so it needs `top` as well — this
 * inline style would otherwise override the class that pins the header vertically,
 * and the pinned columns' headers would scroll away while the others stayed.
 */
export function pinnedStyle<TData>(
  column: Column<TData, unknown>,
  isHeader = false
): React.CSSProperties {
  const pinned = column.getIsPinned();
  if (!pinned) return {};
  return {
    position: "sticky",
    left: pinned === "left" ? column.getStart("left") : undefined,
    right: pinned === "right" ? column.getAfter("right") : undefined,
    top: isHeader ? 0 : undefined,
    // Header above everything, pinned body cells above the scrolling ones.
    zIndex: isHeader ? 30 : 1,
  };
}

/** Marks the boundary between pinned and scrolling columns. */
export function pinnedClass<TData>(
  column: Column<TData, unknown>
): string | undefined {
  const pinned = column.getIsPinned();
  if (!pinned) return undefined;
  return cn(
    pinned === "left" && column.getIsLastColumn("left") && "border-r",
    pinned === "right" && column.getIsFirstColumn("right") && "border-l"
  );
}

interface DataTableRowProps<TData> {
  row: Row<TData>;
  onRowClick?: (row: TData) => void;
  rowActions?: (row: TData) => React.ReactNode;
  /** Highlight class for the whole row, e.g. "row-red" (see globals.css). */
  className?: string;
}

export function DataTableRow<TData>({
  row,
  onRowClick,
  rowActions,
  className,
}: DataTableRowProps<TData>) {
  return (
    <TableRow
      className={cn("group", onRowClick && "cursor-pointer", className)}
      onClick={onRowClick ? () => onRowClick(row.original) : undefined}
    >
      {row.getVisibleCells().map((cell, index) => (
        <TableCell
          key={cell.id}
          style={pinnedStyle(cell.column)}
          className={cn(
            // border-b per cell: with border-separate the row's own border-b
            // doesn't render. Opaque background so columns scrolling past don't
            // show through the pinned ones, which sit in the same stacking layer —
            // which also means the row's hover colour has to be applied per cell.
            // Fully opaque (no /50): a translucent hover would undo exactly the
            // coverage the background provides. A highlighted row overrides this
            // background from globals.css, for the same reason.
            "truncate border-b bg-background group-hover:bg-muted",
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
