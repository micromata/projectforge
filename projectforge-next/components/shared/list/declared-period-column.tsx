"use client";

import type { ColumnDef } from "@tanstack/react-table";
import { DataTableColumnHeader } from "@/components/data-table";
import {
  formatDateRange,
  formatTimestampRange,
  type FormatContext,
} from "@/lib/format";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import { labelKeyFor } from "@/lib/page-def/define-page";
import type { PeriodColumn } from "@/lib/page-def/types";
import { cn } from "@/lib/utils";

/**
 * The column def of a [PeriodColumn]: two date properties of the row in one cell, under the label of
 * the period as a whole.
 *
 * Its id is the period's begin, because that is the property the backend sorts by — with
 * `manualSorting` the id *is* the sort criterion. The accessor yields the same value, so the client
 * side (the column panel, an export) reads the column as the date it is sorted by.
 */
export function periodColumnDef<Row extends ListRow, M extends EntityMetadata>(
  declaration: PeriodColumn<M>,
  metadata: M,
  translate: ((key: string) => string) & { has: (key: string) => boolean },
  format: FormatContext
): ColumnDef<Row> {
  const { begin, end } = declaration;
  const label = translate(
    labelKeyFor(metadata, begin, translate.has, declaration.periodLabelKey)
  );
  const headerLabel = declaration.headerLabelKey
    ? translate(declaration.headerLabelKey)
    : label;
  // Both ends are the same kind of value — the entity declares a period as two `LocalDate`s or two
  // timestamps, never one of each — so the begin decides how the pair is formatted.
  const withTime = metadata.fields[begin]?.dataType === "TIMESTAMP";
  const read = (row: Row, name: string) =>
    (row as unknown as Record<string, unknown>)[name];

  return {
    id: begin,
    accessorFn: (row) => read(row, begin),
    meta: { label, align: "left" },
    size: declaration.size,
    minSize: declaration.minSize,
    header: ({ column, table }) => (
      <DataTableColumnHeader column={column} table={table}>
        {headerLabel}
      </DataTableColumnHeader>
    ),
    cell: ({ row }) => {
      const text = (withTime ? formatTimestampRange : formatDateRange)(
        read(row.original, begin),
        read(row.original, end),
        format
      );
      // Nothing at all for a period with neither end: a dash in every row of a mostly empty column is
      // noise (see AttachmentsSummary).
      if (!text) return null;
      return (
        <span
          className={cn(
            "text-muted-foreground tabular-nums",
            declaration.className
          )}
        >
          {text}
        </span>
      );
    },
  } as ColumnDef<Row>;
}
