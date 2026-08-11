"use client";

import { useCallback } from "react";
import { useTranslations } from "next-intl";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Spinner } from "@/components/shared/spinner";
import { isSelectableTask, type TaskNode } from "@/lib/rs/task";
import { cn } from "@/lib/utils";
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
  className,
}: TaskTreePanelProps) {
  const t = useTranslations();
  const { nodes, grid, filter, setFilter, isLoading, isFetching, toggleNode } =
    useTaskTree({ highlightTaskId, showRootForAdmins, selectMode });

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
          isLoading={isLoading}
          isFetching={isFetching}
          filter={filter}
          onFilterChange={setFilter}
          onToggle={toggle}
          onSelect={select}
        />
      ) : (
        <div className="flex flex-1 items-center justify-center">
          <Spinner />
        </div>
      )}
      <Alert>
        <AlertDescription>{t("task.selectPanel.info")}</AlertDescription>
      </Alert>
    </div>
  );
}
