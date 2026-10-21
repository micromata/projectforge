"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { TaskNode } from "@/lib/rs/task";
import { cn } from "@/lib/utils";

/**
 * The last segment of a [TaskPath]: the selected task itself. Inert by default; with [onOpen] a click
 * opens the tree, as the field's pencil does. The way to the task's time sheets is the consumption bar
 * beside the field (see TaskSelectControl.showConsumption), not this segment.
 */
export function TaskPathCurrent({
  task,
  highlight,
  onOpen,
}: {
  task: TaskNode;
  /** Turquoise and bold, see [TaskPath.highlightCurrent]. */
  highlight: boolean;
  /** Open the tree — the pencil's action, see [TaskPath.onOpen]. Absent while the field is read-only. */
  onOpen?: () => void;
}) {
  const t = useTranslations();
  const className = cn(
    "truncate",
    highlight ? "font-bold text-primary" : "font-medium"
  );
  if (!onOpen) return <span className={className}>{task.title}</span>;
  return (
    <HintTooltip text={t("task.tree.title.select")}>
      <button
        type="button"
        onClick={onOpen}
        className={cn(className, "cursor-pointer hover:underline")}
      >
        {task.title}
      </button>
    </HintTooltip>
  );
}
