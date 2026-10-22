"use client";

import { Fragment } from "react";
import { useTranslations } from "next-intl";
import { OWN_TOOLTIP_ATTR } from "@/components/data-table/use-overflow-tooltip";
import {
  AccessRightsMatrix,
  AccessTypeLegend,
} from "@/components/shared/access/access-rights-matrix";
import { MenuLink } from "@/components/shared/menu-link";
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from "@/components/ui/hover-card";
import { accessListHref } from "@/lib/access-links";
import type { TaskGroupAccess } from "@/lib/rs/task";

/**
 * The admins' groups column of the task tree and the task list: the names of the groups with an access
 * entry on the task, as a link to the access list filtered to exactly this structure element, and on
 * hover each group's rights as the access management shows them — the green checks and red minuses of
 * [AccessRightsMatrix], the access types beside them.
 *
 * A hover card rather than a tooltip: with many groups the matrices are taller than the screen, and the
 * card stays open while the pointer is on it, so it can be scrolled. On touch the tap follows the link,
 * which is the full view anyway.
 *
 * One component for both perspectives, so the two cannot show the same entry differently.
 */
export function TaskGroupAccessCell({
  taskId,
  taskTitle,
  text,
  groups,
}: {
  taskId: number | null | undefined;
  taskTitle?: string | null;
  /** The group names, comma separated (`TaskServicesRest.addGroupAccess`). */
  text?: string | null;
  groups?: TaskGroupAccess[] | null;
}) {
  const t = useTranslations();
  if (!text) return null;
  const link = (
    <MenuLink
      url={
        taskId != null
          ? accessListHref(taskId, taskTitle ?? undefined)
          : undefined
      }
      className="text-primary hover:underline"
      // The row itself is clickable (it opens or expands the task), so a click on the link must not
      // count as picking the row.
      onClick={(event) => event.stopPropagation()}
    >
      {text}
    </MenuLink>
  );
  if (!groups?.length) {
    return <span className="block truncate">{link}</span>;
  }
  return (
    <HoverCard openDelay={400}>
      <HoverCardTrigger asChild>
        {/* Its own hover card, so the table's overflow tooltip stays away (see OWN_TOOLTIP_ATTR). */}
        <span className="block truncate" {...{ [OWN_TOOLTIP_ATTR]: "" }}>
          {link}
        </span>
      </HoverCardTrigger>
      <HoverCardContent
        align="start"
        className="max-h-[60vh] w-auto overflow-y-auto"
      >
        <div className="grid grid-cols-[auto_auto] items-center gap-x-3 gap-y-0.5">
          {groups.map((group) => (
            <Fragment key={group.groupName}>
              <div className="col-span-2 mt-1.5 font-medium first:mt-0">
                {group.groupName}
                {group.recursive && (
                  <span className="font-normal text-muted-foreground">
                    {` (${t("recursive")})`}
                  </span>
                )}
                {group.description && (
                  <div className="font-normal text-muted-foreground">
                    {group.description}
                  </div>
                )}
              </div>
              <AccessTypeLegend rights={group.rights} t={t} />
              <AccessRightsMatrix rights={group.rights} t={t} />
            </Fragment>
          ))}
        </div>
      </HoverCardContent>
    </HoverCard>
  );
}
