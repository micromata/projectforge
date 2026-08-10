"use client";

import { useMemo } from "react";
import type {
  CellContext,
  ColumnDef,
  HeaderContext,
} from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { DataTableColumnHeader, renderCell } from "@/components/data-table";
import { TreeCell } from "@/components/data-table/cells/tree-cell";
import { useFormatContext } from "@/hooks/use-format";
import type { AgGridNode } from "@/lib/dynamic/grid/ag-grid-types";
import { adaptColumnDefs } from "@/lib/dynamic/grid/column-def-adapter";
import type { TaskNode } from "@/lib/rs/task";

/**
 * The tree's columns, from the `columnDefs` of the initial answer.
 *
 * Same shape as `useDynamicGridColumns` and for the same reasons — including the memoisation, which
 * keeps a header from remounting and closing its own popover. It is a hook of its own rather than
 * that one reused, because the structure tree has no DynamicLayout to take `translate` from: its
 * headers arrive already translated, so `next-intl` covers the rest.
 *
 * The one column that differs is the tree column: [TreeCell] gets an `onToggle`, which the generic
 * `renderCell` has no channel for — expanding is this table's own concern, not a cell kind's.
 */
export function useTaskTreeColumns(
  grid: AgGridNode | undefined,
  onToggle: (task: TaskNode) => void
): ColumnDef<TaskNode, unknown>[] {
  const formatCtx = useFormatContext();
  const t = useTranslations();

  return useMemo(() => {
    if (!grid) return [];
    return adaptColumnDefs<TaskNode>(grid).map((column) => {
      const meta = column.meta;
      const label = meta?.label ?? "";
      const spec = meta?.cellSpec;
      const tooltip = meta?.headerTooltip;
      return {
        ...column,
        header: ({ column: col, table }: HeaderContext<TaskNode, unknown>) => (
          <DataTableColumnHeader
            column={col}
            table={table}
            filterKind={meta?.filterKind}
          >
            <span title={tooltip}>{label || tooltip}</span>
          </DataTableColumnHeader>
        ),
        cell: ({ getValue, row }: CellContext<TaskNode, unknown>) => {
          if (!spec) return null;
          const props = {
            spec,
            value: getValue(),
            row: row.original as Record<string, unknown>,
            ctx: formatCtx,
            t,
          };
          return spec.kind === "tree" ? (
            <TreeCell {...props} onToggle={() => onToggle(row.original)} />
          ) : (
            renderCell(props)
          );
        },
      } satisfies ColumnDef<TaskNode, unknown>;
    });
  }, [grid, formatCtx, t, onToggle]);
}
