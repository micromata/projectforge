"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import {
  DataTable,
  DataTableColumnPanel,
  useColumnStatePersistenceByUrl,
  useDataTable,
  useGridStateReset,
  useTableState,
} from "@/components/data-table";
import type { AgGridNode } from "@/lib/dynamic/grid/ag-grid-types";
import { initialStateFrom } from "@/lib/dynamic/grid/initial-state";
import { resolveRestUrl } from "@/lib/dynamic/response-action";
import type { TaskNode, TaskTreeFilter } from "@/lib/rs/task";
import { TaskTreeFilterBar } from "./task-tree-filter";
import { useTaskTreeColumns } from "./use-task-tree-columns";

/** The tree column, whose click means "expand" rather than "select". */
const TREE_COLUMN = "title";

interface TaskTreeTableProps {
  /** The initial answer, which carries the column defs and the grid-state urls. */
  grid: AgGridNode;
  nodes: TaskNode[];
  isLoading: boolean;
  isFetching: boolean;
  filter: TaskTreeFilter;
  onFilterChange: (filter: TaskTreeFilter) => void;
  onToggle: (task: TaskNode) => void;
  onSelect?: (task: TaskNode) => void;
}

/**
 * Filter row and tree, split off the panel because they may only mount once `grid` has arrived: the
 * restored column state seeds TanStack's initial state, which cannot be swapped in later without
 * fighting the user's own edits — the same reason EntityListPage gates on `stored.isPending`. The
 * filter bar comes along because the column panel belongs in its row.
 */
export function TaskTreeTable({
  grid,
  nodes,
  isLoading,
  isFetching,
  filter,
  onFilterChange,
  onToggle,
  onSelect,
}: TaskTreeTableProps) {
  const t = useTranslations();
  const columns = useTaskTreeColumns(grid, onToggle);

  // Guaranteed to be the state stored for the user: the backend folds it into the column defs of the
  // initial answer, and this component only exists once that has arrived.
  const restoredState = useMemo(() => initialStateFrom(grid), [grid]);
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
    grid.onColumnStatesChangedUrl &&
      resolveRestUrl(grid.onColumnStatesChangedUrl),
    persisted
  );
  const resetColumns = useGridStateReset(
    grid.resetGridStateUrl && resolveRestUrl(grid.resetGridStateUrl),
    state,
    suspendPersistence
  );

  return (
    <>
      <div className="flex items-center gap-2">
        <TaskTreeFilterBar filter={filter} onChange={onFilterChange} />
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
            onToggle(row);
          } else {
            onSelect?.(row);
          }
        }}
        className="flex-1"
      />
    </>
  );
}
