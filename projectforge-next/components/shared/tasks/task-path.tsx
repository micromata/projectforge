"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Home01Icon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { TaskNode } from "@/lib/rs/task";
import { cn } from "@/lib/utils";

interface TaskPathProps {
  /** The selected task, or null while nothing is selected. */
  task: TaskNode | null;
  /**
   * Pick an ancestor instead — the shortcut the legacy panel offers as „Strukturelement durch dieses
   * Strukturoberelement ersetzen". `null` means the home button: clear the selection.
   */
  onSelect: (task: TaskNode | null) => void;
  /**
   * Opens the tree, as the field's own button does. Given one, „please select" is a click target
   * itself — it is the widest thing on the row and the first place a pointer goes.
   */
  onOpen?: () => void;
  /**
   * Make an ancestor click open the tree focused on that node instead of selecting it — the one-click
   * drill-down the legacy panel had, where clicking a segment led straight to the booking points
   * beneath it. Off by default: without it a segment click replaces the selection (see [onSelect]).
   * Only takes effect together with [onDrillDown].
   *
   * The ancestor is deliberately *not* selected here: an Oberelement is rarely the task one books
   * against, so committing it just because the tree was opened there — and left again without picking
   * a child — was wrong (a folder would stay selected after closing the tree). Drilling down opens the
   * tree at it; the selection only changes once a task is picked inside.
   */
  openTreeOnAncestorClick?: boolean;
  /**
   * Open the tree focused on the given node — the drill-down [openTreeOnAncestorClick] triggers.
   * Distinct from [onOpen], which opens at the current selection.
   */
  onDrillDown?: (task: TaskNode) => void;
  /** The path may be read but not changed — the shortcuts stop being buttons. */
  disabled?: boolean;
  /** Accessible name of the whole breadcrumb. Defaults to the "please select a task" prompt. */
  label?: string;
  /** Tooltip of the home button. Defaults to the root node's name — the panel that re-roots overrides it. */
  homeTooltip?: string;
  /** Tooltip of an ancestor shortcut. Defaults to "replace by this parent" — re-rooting overrides it. */
  ancestorTooltip?: string;
  /**
   * Show the "please select a task" prompt when nothing is selected. The re-root breadcrumb turns it off:
   * there `null` is the whole tree, a state the home button already stands for, not an empty selection.
   */
  showPlaceholder?: boolean;
  /**
   * Mark the last segment as the current one — turquoise and bold, the way a list marks a distinguished
   * cell. On by default: the select component always highlights the chosen task, so a reader sees at a
   * glance which segment is the task and which are its ancestors. The re-root breadcrumb relies on it for
   * the same reason (the node the tree is rooted at). Pass `false` to render the last segment plain.
   */
  highlightCurrent?: boolean;
}

/**
 * The selected task as its path from the root: home / ancestor / … / task.
 *
 * The ancestors are buttons, since selecting one is how the timesheet moves a booking up the tree
 * without opening the whole panel. The last segment is the current selection: highlighted turquoise by
 * default and inert (see [highlightCurrent]).
 */
export function TaskPath({
  task,
  onSelect,
  onOpen,
  openTreeOnAncestorClick = false,
  onDrillDown,
  disabled,
  label,
  homeTooltip,
  ancestorTooltip,
  showPlaceholder = true,
  highlightCurrent = true,
}: TaskPathProps) {
  const t = useTranslations();
  // `path` holds the ancestors root-first and excludes the task itself (TaskServicesRest.createTask).
  const ancestors = task?.path ?? [];
  const homeText = homeTooltip ?? t("task.tree.rootNode");
  // With the drill-down on, clicking a segment opens the tree focused on it rather than selecting it,
  // so the hint says "open in the tree", not the plain "replace by this parent" shortcut.
  const drillDown = openTreeOnAncestorClick && !!onDrillDown;
  const ancestorText =
    ancestorTooltip ??
    (drillDown
      ? t("task.path.openInTree")
      : t("task.selectPanel.selectAncestorTask.tooltip"));

  return (
    <nav
      aria-label={label ?? t("task.path.pleaseSelectTask")}
      className="flex min-w-0 flex-wrap items-center gap-1 text-xs"
    >
      <HintTooltip text={homeText}>
        <button
          type="button"
          onClick={() => onSelect(null)}
          disabled={disabled}
          aria-label={homeText}
          className="cursor-pointer text-muted-foreground hover:text-foreground disabled:cursor-default disabled:hover:text-muted-foreground"
        >
          <HugeiconsIcon icon={Home01Icon} size={14} />
        </button>
      </HintTooltip>
      {ancestors.map((ancestor) => (
        <span key={ancestor.id} className="flex min-w-0 items-center gap-1">
          <span className="text-muted-foreground">/</span>
          <HintTooltip text={ancestorText}>
            <button
              type="button"
              onClick={() => {
                if (drillDown) onDrillDown!(ancestor);
                else onSelect(ancestor);
              }}
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
          <span
            className={cn(
              "truncate",
              highlightCurrent ? "font-bold text-primary" : "font-medium"
            )}
          >
            {task.title}
          </span>
        </span>
      )}
      {!task &&
        showPlaceholder &&
        (onOpen && !disabled ? (
          <button
            type="button"
            onClick={onOpen}
            className="cursor-pointer text-muted-foreground hover:text-foreground hover:underline"
          >
            {t("task.path.pleaseSelectTask")}
          </button>
        ) : (
          <span className="text-muted-foreground">
            {t("task.path.pleaseSelectTask")}
          </span>
        ))}
    </nav>
  );
}
