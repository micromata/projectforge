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
import { JiraLinkedText } from "@/components/shared/jira/jira-linked-text";
import { useFormatContext } from "@/hooks/use-format";
import type { AgGridNode } from "@/lib/dynamic/grid/ag-grid-types";
import { adaptColumnDefs } from "@/lib/dynamic/grid/column-def-adapter";
import type { TaskNode } from "@/lib/rs/task";

/**
 * The tree's free-text columns whose values may carry JIRA issue keys, linked as Wicket links its list
 * columns. The title stays [TreeCell] (its click expands the node), so it is not among them.
 */
const JIRA_TREE_FIELDS = new Set(["shortDescription", "reference"]);

/**
 * The tree's columns, from the `columnDefs` of the initial answer.
 *
 * Same shape as `useDynamicGridColumns` and for the same reasons — including the memoisation, which
 * keeps a header from remounting and closing its own popover. It is a hook of its own rather than
 * that one reused, because the structure tree has no DynamicLayout to take `translate` from: its
 * headers arrive already translated, so `next-intl` covers the rest.
 *
 * The one column that differs is the tree column: [TreeCell] gets an `onToggle` and the row's
 * `action`, which the generic `renderCell` has no channel for — expanding and adding below a node
 * are this table's own concern, not a cell kind's.
 */
export function useTaskTreeColumns(
  grid: AgGridNode | undefined,
  onToggle: (task: TaskNode) => void,
  /** Whether a cell may link away from the tree, see [CellRenderProps.linkEnabled]. */
  linkEnabled: boolean,
  /** What the row lets one do with the task, rendered behind its title (see [TreeCell]). */
  rowAction?: (task: TaskNode) => React.ReactNode,
  /** The search term the rows were filtered by, highlighted in the cell text (see [CellRenderProps.highlight]). */
  highlight?: string
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
      const field =
        (column as { accessorKey?: string }).accessorKey ?? column.id;
      const jiraLinked = !!field && JIRA_TREE_FIELDS.has(field);
      return {
        ...column,
        header: ({ column: col, table }: HeaderContext<TaskNode, unknown>) => (
          <DataTableColumnHeader
            column={col}
            table={table}
            filterKind={meta?.filterKind}
          >
            <span data-tooltip={tooltip}>{label || tooltip}</span>
          </DataTableColumnHeader>
        ),
        cell: ({ getValue, row }: CellContext<TaskNode, unknown>) => {
          if (!spec) return null;
          // A free-text column: link its JIRA issue keys, as Wicket does in its list columns. The plain
          // string cell is what these columns render anyway, so the value stands in for it.
          if (jiraLinked) {
            const value = getValue();
            return (
              <JiraLinkedText
                text={typeof value === "string" ? value : null}
                highlight={highlight}
              />
            );
          }
          const props = {
            spec,
            value: getValue(),
            row: row.original as Record<string, unknown>,
            ctx: formatCtx,
            t,
            linkEnabled,
            highlight,
          };
          return spec.kind === "tree" ? (
            <TreeCell
              {...props}
              onToggle={() => onToggle(row.original)}
              action={rowAction?.(row.original)}
            />
          ) : (
            renderCell(props)
          );
        },
      } satisfies ColumnDef<TaskNode, unknown>;
    });
  }, [grid, formatCtx, t, onToggle, linkEnabled, rowAction, highlight]);
}
