"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Edit02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { fetchTaskInfo, type TaskNode } from "@/lib/rs/task";
import { TaskConsumptionBar } from "./task-consumption-bar";
import { TaskEditLink } from "./task-edit-link";
import { TaskFavoritesMenu } from "./task-favorites-menu";
import { TaskPath } from "./task-path";
import { TaskSearchPopover } from "./task-search-popover";
import { useRecentTasks } from "./use-recent-tasks";

export interface TaskSelectControlProps {
  /** The picked task, or null while nothing is picked. */
  taskId: number | null;
  /** Accessible name of the button that opens the tree, e.g. the field's label. */
  ariaLabel: string;
  /** The path may be read but not changed (see DeclaredField.readOnly). */
  disabled?: boolean;
  /**
   * Show the link into the task's own edit page. On by default; a filter turns it off, where leaving the
   * list to edit a structure element makes no sense (see FilterTaskField).
   */
  showEditLink?: boolean;
  /** Open the tree — the caller owns the dialog, since it also owns where the value goes. */
  onOpen: () => void;
  /** An ancestor was picked from the path, or the selection was cleared. */
  onSelect: (task: TaskNode | null) => void;
  /** Make a path-segment click open the tree focused on that node instead of selecting it — see [TaskPath]. */
  openTreeOnAncestorClick?: boolean;
  /** Open the tree focused on the given node — the drill-down [openTreeOnAncestorClick] triggers (see [TaskPath]). */
  onDrillDown?: (task: TaskNode) => void;
  /**
   * Show what is booked on the task as the consumption bar after the buttons — which, as everywhere, leads
   * to the time sheets behind it (see TaskConsumptionBar). The order position turns it on.
   */
  showConsumption?: boolean;
  /** The type-ahead finds only tasks a time sheet may be booked on (see TaskSearchPopover). */
  onlyBookable?: boolean;
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
  showEditLink = true,
  onOpen,
  onSelect,
  openTreeOnAncestorClick,
  onDrillDown,
  showConsumption,
  onlyBookable,
}: TaskSelectControlProps) {
  const t = useTranslations();
  const { recordTask } = useRecentTasks();

  const { data: task } = useQuery({
    queryKey: ["taskInfo", taskId],
    queryFn: ({ signal }) => fetchTaskInfo(taskId!, signal),
    enabled: taskId != null,
    staleTime: Infinity,
    // The consumption changes with every booking, and nothing invalidates the shared query on a saved or
    // deleted time sheet: refetch it whenever the field mounts, so the bar isn't the first fetch's figure.
    refetchOnMount: showConsumption ? "always" : false,
  });

  /**
   * The one funnel for a pick from the search popover *and* from the breadcrumb: both feed [onSelect]
   * here, so recording it once — for every consumer of this control (the form field, the timesheet
   * kost2 picker, the filter row, the wizard) — keeps the recent quick-picks current. Clearing (null)
   * is not a pick. The tree dialog records its own pick, since it is a sibling of this control, not a
   * child (see TaskSelectModal).
   */
  const select = (node: TaskNode | null) => {
    if (node != null) recordTask(node.id);
    onSelect(node);
  };

  return (
    <div className="flex min-w-0 items-center gap-2">
      {/* Only as wide as the path itself, so the buttons sit directly after it rather than at the row's
          right edge; still `min-w-0` so a long path truncates instead of pushing them off-screen. */}
      <div className="min-w-0 shrink">
        <TaskPath
          task={(taskId != null && task) || null}
          onSelect={select}
          onOpen={onOpen}
          openTreeOnAncestorClick={openTreeOnAncestorClick}
          onDrillDown={onDrillDown}
          disabled={disabled}
        />
      </div>
      {/* Before the tree button, in the order Wicket's select panel has the two: the term first, the
          tree for when the name is not what the user knows. */}
      <TaskSearchPopover
        ariaLabel={ariaLabel}
        disabled={disabled}
        onlyBookable={onlyBookable}
        onSelect={select}
      />
      <TaskFavoritesMenu
        taskId={taskId}
        disabled={disabled}
        onSelect={select}
      />
      <HintTooltip text={t("task.tree.title.select")}>
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
      </HintTooltip>
      {/* Leads to the task itself, where its timesheets are — see TaskEditLink. Not for a task the user
          sees by a booking grant only: its page is not theirs to open. */}
      {showEditLink && task?.bookingGrant == null && (
        <TaskEditLink taskId={taskId} />
      )}
      {showConsumption && taskId != null && task && (
        <div className="w-24 shrink-0">
          <TaskConsumptionBar task={task} />
        </div>
      )}
    </div>
  );
}
