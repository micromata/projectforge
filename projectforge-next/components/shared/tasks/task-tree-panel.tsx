"use client";

import { useCallback } from "react";
import { useTranslations } from "next-intl";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { MarkdownText } from "@/components/shared/markdown-text";
import { Spinner } from "@/components/shared/spinner";
import { isSelectableTask, type TaskNode } from "@/lib/rs/task";
import { cn } from "@/lib/utils";
import { TaskRootBreadcrumb } from "./task-root-breadcrumb";
import { TaskTreeTable } from "./task-tree-table";
import type { TaskTreePanelProps } from "./types";
import { useTaskTree } from "./use-task-tree";

/**
 * The structure tree as a table: filter, tree and the hint how to select a folder.
 *
 * Reusable on purpose — the `/next/taskTree` page and the select field are the same panel with
 * different props. Which is why it takes `onSelect` instead of navigating itself, and why the page's
 * actions are not in here: they sit in that page's header row, where the list's own sit too, and act on
 * the state the page then owns (see TaskTreePanelProps.tree).
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
  rootNavigable,
  pageMode,
  tree,
  className,
}: TaskTreePanelProps) {
  const t = useTranslations();
  // The hook is called either way — hooks are not conditional — but with the same options the caller
  // passed, so both instances share one query and no second request goes out. Only the state of the
  // caller's instance is read, since that is the one its buttons act on.
  const own = useTaskTree({
    highlightTaskId,
    showRootForAdmins,
    selectMode,
    rootNavigable,
  });
  const {
    nodes,
    grid,
    filter,
    setFilter,
    isLoading,
    isFetching,
    toggleNode,
    rootTaskId,
    navigateToRoot,
    goBack,
    goForward,
    canGoBack,
    canGoForward,
  } = tree ?? own;

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
          // Below the search row rather than above it (see TaskTreeTable): the breadcrumb captions the
          // tree it re-roots, so it belongs next to the tree, not stacked over the filter toolbar.
          breadcrumb={
            rootNavigable && (
              <TaskRootBreadcrumb
                rootTaskId={rootTaskId}
                onNavigate={navigateToRoot}
                onBack={goBack}
                onForward={goForward}
                canGoBack={canGoBack}
                canGoForward={canGoForward}
              />
            )
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
          {/* The hint carries markdown (a lead-in plus the key bindings as a list); rendered like
              every other `*.info` text in the bundle rather than as one flat paragraph. */}
          <MarkdownText
            text={t(pageMode ? "task.tree.info" : "task.selectPanel.info")}
          />
        </AlertDescription>
      </Alert>
    </div>
  );
}
