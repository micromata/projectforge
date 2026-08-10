"use client";

import { useCallback, useMemo } from "react";
import { useTranslations } from "next-intl";
import {
  DataTable,
  DataTableColumnPanel,
  useColumnStatePersistenceByUrl,
  useDataTable,
  useGridStateReset,
  useTableState,
} from "@/components/data-table";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { initialStateFrom } from "@/lib/dynamic/grid/initial-state";
import { resolveRestUrl } from "@/lib/dynamic/response-action";
import type { TaskNode } from "@/lib/rs/task";
import { cn } from "@/lib/utils";
import { TaskTreeFilterBar } from "./task-tree-filter";
import type { TaskTreePanelProps } from "./types";
import { useTaskTree } from "./use-task-tree";
import { useTaskTreeColumns } from "./use-task-tree-columns";

/** The tree column, whose click means "expand" rather than "select". */
const TREE_COLUMN = "title";

/**
 * The structure tree as a table: filter, tree and the hint how to select a folder.
 *
 * Reusable on purpose — the `/next/taskTree` page and the select field are the same panel with
 * different props. Which is why it takes `onSelect` instead of navigating itself.
 *
 * The columns and their stored widths come from the backend (see useTaskTree), so the panel needs no
 * column declaration of its own; the state is persisted through the two urls of the response rather
 * than by category, because the tree is served by a service endpoint and not by `/rs/<category>/…`.
 */
export function TaskTreePanel({
  highlightTaskId,
  onSelect,
  showRootForAdmins,
  className,
}: TaskTreePanelProps) {
  const t = useTranslations();
  const { nodes, grid, filter, setFilter, isLoading, isFetching, toggleNode } =
    useTaskTree({ highlightTaskId, showRootForAdmins });

  const toggle = useCallback(
    (task: TaskNode) => {
      if (task.treeStatus === "LEAF") return;
      toggleNode(task.id, task.treeStatus !== "OPENED");
    },
    [toggleNode]
  );
  const columns = useTaskTreeColumns(grid, toggle);

  // The first answer already carries the user's restored state, so it is there on the first render
  // that has columns at all — which is the same one, since both come from that answer.
  const restoredState = useMemo(
    () => (grid ? initialStateFrom(grid) : undefined),
    [grid]
  );
  const state = useTableState({ restoredState });
  const table = useDataTable<TaskNode>({
    columns,
    data: nodes,
    sorting: state.sorting,
    onSortingChange: state.setSorting,
    columnFilters: state.columnFilters,
    onColumnFiltersChange: state.setColumnFilters,
    columnVisibility: state.columnVisibility,
    onColumnVisibilityChange: state.setColumnVisibility,
    columnPinning: state.columnPinning,
    onColumnPinningChange: state.setColumnPinning,
    columnSizing: state.columnSizing,
    onColumnSizingChange: state.setColumnSizing,
    columnOrder: state.columnOrder,
    onColumnOrderChange: state.setColumnOrder,
    enableColumnFilters: true,
    enableColumnResizing: true,
    manualSorting: false,
    getRowId: (row, index) => String(row.id ?? index),
  });

  // Memoised so a render that changed no column state doesn't hand the hook a new object — it
  // compares by serialization, but its effect would still re-run and re-arm the debounced write.
  const persisted = useMemo(
    () => ({
      sorting: state.sorting,
      columnVisibility: state.columnVisibility,
      columnPinning: state.columnPinning,
      columnSizing: state.columnSizing,
      columnOrder: state.columnOrder,
    }),
    [
      state.sorting,
      state.columnVisibility,
      state.columnPinning,
      state.columnSizing,
      state.columnOrder,
    ]
  );
  const suspendPersistence = useColumnStatePersistenceByUrl(
    grid?.onColumnStatesChangedUrl &&
      resolveRestUrl(grid.onColumnStatesChangedUrl),
    persisted
  );
  const resetColumns = useGridStateReset(
    grid?.resetGridStateUrl && resolveRestUrl(grid.resetGridStateUrl),
    state,
    suspendPersistence
  );

  return (
    <div className={cn("flex min-h-0 flex-1 flex-col gap-2", className)}>
      <div className="flex items-center gap-2">
        <TaskTreeFilterBar filter={filter} onChange={setFilter} />
        <DataTableColumnPanel
          table={table}
          onReset={resetColumns}
          className="h-8 rounded-full px-2.5 text-xs"
        />
      </div>
      <DataTable<TaskNode>
        table={table}
        columns={columns}
        data={nodes}
        isLoading={isLoading}
        isFetching={isFetching}
        emptyState={t("task.selectPanel.noTasksFound")}
        // A folder's title expands it, every other column selects it — the rule the hint below
        // states, and the reason DataTable knows about cells at all.
        onCellClick={(row, columnId) => {
          if (columnId === TREE_COLUMN && row.treeStatus !== "LEAF") {
            toggle(row);
          } else {
            onSelect?.(row);
          }
        }}
        className="flex-1"
      />
      <Alert>
        <AlertDescription>{t("task.selectPanel.info")}</AlertDescription>
      </Alert>
    </div>
  );
}
