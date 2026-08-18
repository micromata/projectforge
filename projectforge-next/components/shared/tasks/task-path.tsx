"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Home01Icon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { TaskNode } from "@/lib/rs/task";

interface TaskPathProps {
  /** The selected task, or null while nothing is selected. */
  task: TaskNode | null;
  /**
   * Pick an ancestor instead — the shortcut the legacy panel offers as „Strukturelement durch dieses
   * Strukturoberelement ersetzen". `null` means the home button: clear the selection.
   */
  onSelect: (task: TaskNode | null) => void;
  /** The path may be read but not changed — the shortcuts stop being buttons. */
  disabled?: boolean;
}

/**
 * The selected task as its path from the root: home / ancestor / … / task.
 *
 * The ancestors are buttons, since selecting one is how the timesheet moves a booking up the tree
 * without opening the whole panel. The last segment is the current selection and does nothing.
 */
export function TaskPath({ task, onSelect, disabled }: TaskPathProps) {
  const t = useTranslations();
  // `path` holds the ancestors root-first and excludes the task itself (TaskServicesRest.createTask).
  const ancestors = task?.path ?? [];

  return (
    <nav
      aria-label={t("task.path.pleaseSelectTask")}
      className="flex min-w-0 flex-wrap items-center gap-1 text-xs"
    >
      <HintTooltip text={t("task.tree.rootNode")}>
        <button
          type="button"
          onClick={() => onSelect(null)}
          disabled={disabled}
          aria-label={t("task.tree.rootNode")}
          className="cursor-pointer text-muted-foreground hover:text-foreground disabled:cursor-default disabled:hover:text-muted-foreground"
        >
          <HugeiconsIcon icon={Home01Icon} size={14} />
        </button>
      </HintTooltip>
      {ancestors.map((ancestor) => (
        <span key={ancestor.id} className="flex min-w-0 items-center gap-1">
          <span className="text-muted-foreground">/</span>
          <HintTooltip text={t("task.selectPanel.selectAncestorTask.tooltip")}>
            <button
              type="button"
              onClick={() => onSelect(ancestor)}
              disabled={disabled}
              className="max-w-40 cursor-pointer truncate text-muted-foreground hover:text-foreground hover:underline disabled:cursor-default disabled:hover:text-muted-foreground disabled:hover:no-underline"
            >
              {ancestor.title}
            </button>
          </HintTooltip>
        </span>
      ))}
      {task && (
        <span className="flex min-w-0 items-center gap-1">
          <span className="text-muted-foreground">/</span>
          <span className="truncate font-medium">{task.title}</span>
        </span>
      )}
      {!task && (
        <span className="text-muted-foreground">
          {t("task.path.pleaseSelectTask")}
        </span>
      )}
    </nav>
  );
}
