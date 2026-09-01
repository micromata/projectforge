"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { DataTableColumnHeader, renderCell } from "@/components/data-table";
import { useFormatContext } from "@/hooks/use-format";
import type { DataObject } from "@/lib/dynamic/path";
import type { AgGridNode } from "@/lib/dynamic/grid/ag-grid-types";
import { adaptColumnDefs } from "@/lib/dynamic/grid/column-def-adapter";
import { useDynamicLayout } from "../../dynamic-context";

/**
 * Adds the rendered parts to the adapted column defs: the header (label, sort and
 * filter affordances) and the cell (the formatter registry).
 *
 * Memoised on purpose, like `useBooksColumns`: a new `header` identity remounts
 * the header cell, which closes an open filter popover on the very click that
 * opened it. `translate` is stable (see DynamicLayoutProvider), `t` is stable per
 * locale and `formatCtx` comes from a memo, so the dependency list holds.
 */
export function useDynamicGridColumns(
  grid: AgGridNode
): ColumnDef<DataObject, unknown>[] {
  const { translate } = useDynamicLayout();
  const formatCtx = useFormatContext();
  // A cell's accessible names ("yes", "rating", ...) are app texts from the
  // generated catalogs, not part of this layout's own translations.
  const t = useTranslations();

  return useMemo(
    () =>
      adaptColumnDefs(grid).map((column) => {
        const meta = column.meta;
        // headerName is already translated by the backend for most columns; going
        // through `translate` covers the ones sending a plain i18n key.
        const label = meta?.label ? translate(meta.label) : "";
        const spec = meta?.cellSpec;
        const tooltip = meta?.headerTooltip;
        return {
          ...column,
          meta: { ...meta, label: label || tooltip },
          header: ({ column: col, table }) => (
            <DataTableColumnHeader
              column={col}
              table={table}
              filterKind={meta?.filterKind}
            >
              {/* An icon-only header (attachments) sends an empty name and a
                  tooltip instead; without it the column would be unnamed. */}
              <span data-tooltip={tooltip}>{label || tooltip}</span>
            </DataTableColumnHeader>
          ),
          cell: ({ getValue, row, table }) =>
            spec
              ? renderCell({
                  spec,
                  value: getValue(),
                  row: row.original,
                  ctx: formatCtx,
                  t,
                  // The active search term, off the table meta (see useDataTable), so every list
                  // cell highlights the match without this builder knowing the term itself.
                  highlight: table.options.meta?.highlight,
                })
              : null,
        } satisfies ColumnDef<DataObject, unknown>;
      }),
    [grid, translate, formatCtx, t]
  );
}
