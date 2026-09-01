"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Edit02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { fetchTaskInfo, type TaskNode } from "@/lib/rs/task";
import { TaskEditLink } from "./task-edit-link";
import { TaskPath } from "./task-path";
import { TaskSearchPopover } from "./task-search-popover";

export interface TaskSelectControlProps {
  /** The picked task, or null while nothing is picked. */
  taskId: number | null;
  /** Accessible name of the button that opens the tree, e.g. the field's label. */
  ariaLabel: string;
  /** The path may be read but not changed (see DeclaredField.readOnly). */
  disabled?: boolean;
  /** Open the tree — the caller owns the dialog, since it also owns where the value goes. */
  onOpen: () => void;
  /** An ancestor was picked from the path, or the selection was cleared. */
  onSelect: (task: TaskNode | null) => void;
  /** Make a path-segment click open the tree scoped to that node, not just select it — see [TaskPath]. */
  openTreeOnAncestorClick?: boolean;
}

/**
 * The visible half of a task picker: the breadcrumb path of the current value, the type-ahead, the
 * button that opens the tree, and the link into the task itself.
 *
 * Separate from [TaskSelectField] because the field only adds the form layer around it — the wizard
 * has no form context and picks a task the same way (see TaskWizard). The task behind the id is
 * fetched here, so a caller holding nothing but an id gets the whole path.
 */
export function TaskSelectControl({
  taskId,
  ariaLabel,
  disabled,
  onOpen,
  onSelect,
  openTreeOnAncestorClick,
}: TaskSelectControlProps) {
  const t = useTranslations();

  const { data: task } = useQuery({
    queryKey: ["taskInfo", taskId],
    queryFn: ({ signal }) => fetchTaskInfo(taskId!, signal),
    enabled: taskId != null,
    staleTime: Infinity,
  });

  return (
    <div className="flex min-w-0 items-center gap-2">
      {/* Only as wide as the path itself, so the buttons sit directly after it rather than at the row's
          right edge; still `min-w-0` so a long path truncates instead of pushing them off-screen. */}
      <div className="min-w-0 shrink">
        <TaskPath
          task={(taskId != null && task) || null}
          onSelect={(node) => onSelect(node)}
          onOpen={onOpen}
          openTreeOnAncestorClick={openTreeOnAncestorClick}
          disabled={disabled}
        />
      </div>
      {/* Before the tree button, in the order Wicket's select panel has the two: the term first, the
          tree for when the name is not what the user knows. */}
      <TaskSearchPopover
        ariaLabel={ariaLabel}
        disabled={disabled}
        onSelect={onSelect}
      />
      <Button
        type="button"
        variant="outline"
        size="icon"
        disabled={disabled}
        aria-label={t("task.tree.title.select") + " " + ariaLabel}
        onClick={onOpen}
        className="size-7 shrink-0"
      >
        <HugeiconsIcon icon={Edit02Icon} size={14} />
      </Button>
      {/* Leads to the task itself, where its timesheets are — see TaskEditLink. */}
      <TaskEditLink taskId={taskId} />
    </div>
  );
}
