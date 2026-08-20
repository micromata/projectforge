"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { LinkSquare02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";

/**
 * The way from a selected task to that task's own page — the shortcut the legacy order form offered
 * from a position, and from where the timesheets booked on the task are reachable.
 *
 * Opens in a new tab on purpose: this sits inside an edit form, and following it in the same tab
 * would throw away everything typed so far.
 *
 * The target is Wicket's page and is spelled out here, not taken from `listMeta.legacyEditPage` as
 * everywhere else: that one answers `react/task/edit/:id` for the task (the category isn't migrated,
 * so `NextMigration.legacyApp` falls back to the React app), and the React form is a plain UILayout
 * form without the one action this link exists for — „Zeitberichte anzeigen" is a content menu entry
 * of `TaskEditPage`. Once the task page itself is migrated, this has to point at its next route
 * instead; see MIGRATION.md.
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
        {/* A plain anchor, not next/link: the target belongs to another app and needs a full load. */}
        <a
          href={toAbsoluteUrl(resolveMenuUrl(`wa/taskEdit?id=${taskId}`))}
          target="_blank"
          rel="noopener noreferrer"
        >
          <HugeiconsIcon icon={LinkSquare02Icon} size={14} />
        </a>
      </Button>
    </HintTooltip>
  );
}
