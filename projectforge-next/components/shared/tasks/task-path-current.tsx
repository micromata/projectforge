"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import type { TaskNode } from "@/lib/rs/task";
import { timesheetListHref } from "@/lib/timesheet-links";
import { cn } from "@/lib/utils";

/**
 * The last segment of a [TaskPath]: the selected task itself. Inert by default; with [linkToTimesheets]
 * it leads to the time sheets booked on the task — the one-click jump the legacy order form offered from
 * a position's structure element.
 *
 * A plain anchor in a new tab, as [TaskEditLink]: the path sits inside an edit form, and following it in
 * the same tab would unmount the form and throw away everything typed so far. Still a link when the path
 * is read-only — looking at the time sheets does not change the field.
 */
export function TaskPathCurrent({
  task,
  highlight,
  linkToTimesheets,
}: {
  task: TaskNode;
  /** Turquoise and bold, see [TaskPath.highlightCurrent]. */
  highlight: boolean;
  linkToTimesheets: boolean;
}) {
  const t = useTranslations();
  const className = cn(
    "truncate",
    highlight ? "font-bold text-primary" : "font-medium"
  );
  if (!linkToTimesheets) {
    return <span className={className}>{task.title}</span>;
  }
  const label = t("task.menu.showTimesheets");

  return (
    <HintTooltip text={label}>
      <a
        href={toAbsoluteUrl(
          resolveMenuUrl(timesheetListHref(task.id, task.title ?? undefined))
        )}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`${label}: ${task.title ?? ""}`}
        className={cn(className, "hover:underline")}
      >
        {task.title}
      </a>
    </HintTooltip>
  );
}
