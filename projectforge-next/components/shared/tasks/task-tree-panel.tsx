"use client";

import { useCallback } from "react";
import { useTranslations } from "next-intl";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/shared/spinner";
import { isSelectableTask, type TaskNode } from "@/lib/rs/task";
import { cn } from "@/lib/utils";
import { TaskTreeActionBar } from "./task-tree-action-bar";
import { TaskTreeTable } from "./task-tree-table";
import type { TaskTreePanelProps } from "./types";
import { useTaskTree } from "./use-task-tree";

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
  rootSelectable,
  selectMode,
  pageMode,
  className,
}: TaskTreePanelProps) {
  const t = useTranslations();
  const {
    nodes,
    grid,
    filter,
    setFilter,
    resetFilter,
    isLoading,
    isFetching,
    toggleNode,
  } = useTaskTree({ highlightTaskId, showRootForAdmins, selectMode });

  const toggle = useCallback(
    (task: TaskNode) => {
      if (task.treeStatus === "LEAF") return;
      toggleNode(task.id, task.treeStatus !== "OPENED");
    },
    [toggleNode]
  );

  // Dropping the root here rather than in the table keeps the rule in one place: it holds for the
  // click handler and for anything else that may come to select a row.
  const select = useCallback(
    (task: TaskNode) => {
      if (!rootSelectable && !isSelectableTask(task)) return;
      onSelect?.(task);
    },
    [onSelect, rootSelectable]
  );

  return (
    <div className={cn("flex min-h-0 flex-1 flex-col gap-2", className)}>
      {/* The table only mounts once the initial answer is in: it seeds its column state from that
          answer, and TanStack's initial state cannot be replaced afterwards (see TaskTreeTable). */}
      {grid ? (
        <TaskTreeTable
          grid={grid}
          nodes={nodes}
          highlightTaskId={highlightTaskId}
          isLoading={isLoading}
          isFetching={isFetching}
          filter={filter}
          onFilterChange={setFilter}
          onToggle={toggle}
          onSelect={select}
          pageActions={pageMode}
          // A select popover picks a task for a form — following the consumption bar to the time
          // sheets would leave it (see TaskTreeTableProps.linkEnabled).
          linkEnabled={!selectMode}
          actionBar={
            pageMode && <TaskTreeActionBar onFilterReset={resetFilter} />
          }
        />
      ) : (
        <div className="flex flex-1 items-center justify-center">
          <Spinner />
        </div>
      )}
      {/* Two hints for two meanings of a click: on the page a row opens the task's own form, in a
          select field it picks the task for something else. `task.tree.info` is the text Wicket puts
          below its tree page (TaskTreePage's "info" label). */}
      <Alert>
        <AlertDescription>
          {t(pageMode ? "task.tree.info" : "task.selectPanel.info")}
        </AlertDescription>
      </Alert>
    </div>
  );
}
