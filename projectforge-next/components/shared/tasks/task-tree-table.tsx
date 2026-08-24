"use client";

import { useMemo } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
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
import { deletedRowClass } from "@/lib/dynamic/grid/row-class";
import { resolveRestUrl } from "@/lib/dynamic/response-action";
import type { TaskNode, TaskTreeFilter } from "@/lib/rs/task";
import { cn } from "@/lib/utils";
import { TASK_TREE_ROUTE, newTaskHref } from "./task-routes";
import { TaskTreeFilterBar } from "./task-tree-filter";
import { useTaskTreeColumns } from "./use-task-tree-columns";

/** The tree column, whose click means "expand" rather than "select". */
const TREE_COLUMN = "title";

interface TaskTreeTableProps {
  /** The initial answer, which carries the column defs and the grid-state urls. */
  grid: AgGridNode;
  nodes: TaskNode[];
  /** The task to mark and scroll to — the one the panel was opened at (see TaskTreePanelProps). */
  highlightTaskId?: number | null;
  isLoading: boolean;
  isFetching: boolean;
  filter: TaskTreeFilter;
  onFilterChange: (filter: TaskTreeFilter) => void;
  onToggle: (task: TaskNode) => void;
  onSelect?: (task: TaskNode) => void;
  /**
   * Offer "add a subtask" per row, and the handbook link beside the search field. Only the tree page
   * does: a select popover is for picking a task, not for creating one.
   */
  pageActions?: boolean;
  /**
   * Let a cell link out of the tree — the consumption bar to the task's time sheets. Off in a select
   * popover, where the link would leave the form the task is being picked for (Wicket does the same,
   * see TaskListPage.getConsumptionBarPanel).
   */
  linkEnabled?: boolean;
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
  highlightTaskId,
  isLoading,
  isFetching,
  filter,
  onFilterChange,
  onToggle,
  onSelect,
  pageActions,
  linkEnabled = true,
}: TaskTreeTableProps) {
  const t = useTranslations();
  const columns = useTaskTreeColumns(grid, onToggle, linkEnabled);

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
      {/* On the page this is the last row of the page's header, so it spans the whole width and carries
          the header's one bottom border - the place the list has it too (see ListToolbar). `-mx-4`
          against the padding of the page's content area, which the popover does not have. */}
      <div
        className={cn(
          "flex items-center gap-2",
          pageActions && "-mx-4 border-b bg-background px-4 pb-2.5"
        )}
      >
        <TaskTreeFilterBar
          filter={filter}
          onChange={onFilterChange}
          showSearchHelp={pageActions}
        />
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
        rowClassName={deletedRowClass}
        // The server already opened the ancestors of this task (see useTaskTree); marking it and
        // scrolling to it is what makes it findable in a tree of thousands. No scope: the dialog is
        // reopened in order to see the selected task, so it scrolls there every time.
        highlightRowId={highlightTaskId}
        // A folder's title expands it, every other column selects it — the rule the hint below
        // states, and the reason DataTable knows about cells at all.
        onCellClick={(row, columnId) => {
          if (columnId === TREE_COLUMN && row.treeStatus !== "LEAF") {
            onToggle(row);
          } else {
            onSelect?.(row);
          }
        }}
        // Not in Wicket, whose tree can only add below the root — where the new task then has to be
        // moved by hand. The parent travels as a parameter of the preset, because only the backend can
        // resolve what hangs on it (see newTaskHref).
        rowActions={
          pageActions ? (row) => <AddSubtaskAction task={row} /> : undefined
        }
        className="flex-1"
      />
    </>
  );
}

/**
 * "Add a subtask below this one" — a link, so it opens in a new tab like any other and the row's own
 * click (which selects the task) is unaffected.
 *
 * Never for the root: it is the tree's anchor and adding below it is what the page's own `+` does, so
 * the row would offer the same thing twice.
 */
function AddSubtaskAction({ task }: { task: TaskNode }) {
  const t = useTranslations();
  if (task.root === true) return null;
  const label = `${t("task.title.add")}: ${task.title ?? ""}`;

  return (
    <HintTooltip text={`${t("task.title.add")} (${t("task.parentTask")})`}>
      <Button asChild variant="ghost" size="icon" aria-label={label}>
        <Link
          href={newTaskHref({
            parentTaskId: task.id,
            returnTo: TASK_TREE_ROUTE,
          })}
        >
          <HugeiconsIcon icon={PlusSignIcon} size={14} strokeWidth={2.5} />
        </Link>
      </Button>
    </HintTooltip>
  );
}
