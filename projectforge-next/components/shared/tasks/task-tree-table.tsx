"use client";

import { useCallback, useMemo, type ReactNode } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import {
  DataTable,
  DataTableColumnPanel,
  rememberMarkedRow,
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
import {
  TASK_TREE_ROUTE,
  TASK_TREE_VIEW_SCOPE,
  newTaskHref,
} from "./task-routes";
import { TaskTreeFilterBar } from "./task-tree-filter";
import { useTaskTreeColumns } from "./use-task-tree-columns";
import { useTreeKeyboard } from "./use-tree-keyboard";

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
  /**
   * The root breadcrumb of a re-rootable panel, rendered right below the search row so it reads as a
   * caption of the tree beneath it rather than as a second toolbar above the filter (see TaskTreePanel).
   */
  breadcrumb?: ReactNode;
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
  breadcrumb,
}: TaskTreeTableProps) {
  const t = useTranslations();
  // Behind the task's title rather than in DataTable's trailing actions column, which on a tree ten
  // columns wide is nowhere near the title it belongs to (see TreeCell). Not in Wicket, whose tree
  // can only add below the root — where the new task then has to be moved by hand. The parent travels
  // as a parameter of the preset, because only the backend can resolve what hangs on it (see
  // newTaskHref). Memoised, or every render would hand useTaskTreeColumns a new function and rebuild
  // the columns.
  const rowAction = useMemo(
    () =>
      pageActions
        ? (task: TaskNode) => <AddSubtaskAction task={task} />
        : undefined,
    [pageActions]
  );
  const columns = useTaskTreeColumns(grid, onToggle, linkEnabled, rowAction);

  // The one select path, for mouse (onCellClick below) and keyboard alike, so both remember the row —
  // on the *page* only: opening a task there is what a Cancel or a browser-back returns to, so the
  // tree marks it again the way a save's `?highlightId=` does (see the tree page and the shared list
  // memory, rememberMarkedRow/recallMarkedRowId). A select popover picks a value and never returns to
  // a highlight, so it writes nothing — and only a select remembers, never a toggle, which would light
  // up a node just from browsing (recallMarkedRowId is read on every render). `onSelect` is optional,
  // so where the panel passes none the key is a no-op.
  const selectTask = useCallback(
    (task: TaskNode) => {
      if (pageActions) rememberMarkedRow(TASK_TREE_VIEW_SCOPE, String(task.id));
      onSelect?.(task);
    },
    [pageActions, onSelect]
  );
  // File-explorer keys over the tree: ↑/↓ move the focus, →/← expand and collapse, Enter opens (or,
  // in a select popover, picks) the focused element.
  const keyboardNav = useTreeKeyboard(
    nodes,
    onToggle,
    selectTask,
    highlightTaskId
  );

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
    // The tree is never paged: it is one connected structure, shown whole as Wicket's does — so the
    // pagination row model is left off (all rows render) and the pagination bar is hidden below.
    manualPagination: true,
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
      {breadcrumb}
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
        // The tree is a file-explorer view: the more of a deep structure fits on screen, the better
        // (see Wicket's taskTree), so its rows are tighter than an ordinary list's.
        dense
        // A tree is never paged (see manualPagination above), so the bar would only ever read
        // "1-N of N" over the whole structure.
        showPagination={false}
        // On its own page the tree stands vertically complete: no inner scroller, the page scrolls, so
        // the whole structure is there to scroll through and the help hint sits below it in the flow.
        // In a select dialog it stays a bounded, scrolling box (autoHeight off).
        autoHeight={pageActions}
        keyboardNav={keyboardNav}
        // A folder's title expands it, every other column selects it — the rule the hint below
        // states, and the reason DataTable knows about cells at all.
        onCellClick={(row, columnId) => {
          if (columnId === TREE_COLUMN && row.treeStatus !== "LEAF") {
            onToggle(row);
          } else {
            selectTask(row);
          }
        }}
        // In a dialog the table fills the bounded body (flex-1); on its own page it takes its natural
        // height instead, so the page - not the table - scrolls (see autoHeight).
        className={pageActions ? undefined : "flex-1"}
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
      {/* icon-xs, not the default icon size: the button sits inline in the title cell and, even
          revealed only on hover, its box sets the row's height. A larger one would make every tree
          row taller than its text needs — the tree is meant to be dense (see Wicket's taskTree). */}
      <Button asChild variant="ghost" size="icon-xs" aria-label={label}>
        <Link
          href={newTaskHref({
            parentTaskId: task.id,
            returnTo: TASK_TREE_ROUTE,
          })}
        >
          <HugeiconsIcon icon={PlusSignIcon} size={12} strokeWidth={2.5} />
        </Link>
      </Button>
    </HintTooltip>
  );
}
