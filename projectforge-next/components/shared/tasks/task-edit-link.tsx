"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { LinkSquare02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import { taskHref } from "./task-routes";

/**
 * The way from a selected task to that task's own page — the shortcut the legacy order form offered
 * from a position, and from where the timesheets booked on the task are reachable.
 *
 * The target is the migrated next form (`task` is in `NextMigration.MIGRATED`), which carries the
 * five cross-links of Wicket's content menu, „Zeitberichte anzeigen" among them — the one action this
 * link exists for. Until the migration this pointed at `wa/taskEdit`, because the React form was a
 * plain UILayout form without that action.
 *
 * Still a plain anchor in a new tab, not `next/link`: this sits inside an edit form, and following it
 * in the same tab — even to a route of this app — would unmount the form and throw away everything
 * typed so far. The absolute url (`toAbsoluteUrl`) is what a new tab needs.
 */
export function TaskEditLink({ taskId }: { taskId: number | null }) {
  const t = useTranslations();
  if (taskId == null) return null;
  const label = t("task.title.edit");

  return (
    <HintTooltip text={label}>
      <Button
        asChild
        variant="outline"
        size="icon"
        aria-label={label}
        className="size-7 shrink-0"
      >
        <a
          href={toAbsoluteUrl(resolveMenuUrl(`next${taskHref(taskId)}`))}
          target="_blank"
          rel="noopener noreferrer"
        >
          <HugeiconsIcon icon={LinkSquare02Icon} size={14} />
        </a>
      </Button>
    </HintTooltip>
  );
}
