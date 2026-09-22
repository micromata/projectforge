"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Calendar03Icon,
  CircleArrowReload01Icon,
  MoreHorizontalIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { LegacyMenuItem } from "@/components/shared/legacy-page-link";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";

interface CalendarMoreMenuProps {
  onRefresh: () => void;
}

/**
 * The overflow menu: a manual refresh in place of the legacy page reload, and the list of team
 * calendars — still a legacy page (`teamCal` is not migrated), so a plain anchor that leaves the app.
 * The colour settings now live in the gear dialog (CalendarColorSettings), not on a separate page.
 */
export function CalendarMoreMenu({ onRefresh }: CalendarMoreMenuProps) {
  const t = useTranslations();
  const teamCalList = toAbsoluteUrl(resolveMenuUrl("react/teamCal"));

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          aria-label={t("more")}
          className="size-8"
        >
          <HugeiconsIcon icon={MoreHorizontalIcon} size={16} />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-72">
        <HintTooltip
          side="left"
          text={t("plugins.teamcal.calendar.refresh.tooltip")}
        >
          <DropdownMenuItem onSelect={onRefresh}>
            <HugeiconsIcon icon={CircleArrowReload01Icon} size={14} />
            {t("reload")}
          </DropdownMenuItem>
        </HintTooltip>
        <DropdownMenuItem asChild>
          <a href={teamCalList}>
            <HugeiconsIcon icon={Calendar03Icon} size={14} />
            {t("menu.plugins.teamcal")}
          </a>
        </DropdownMenuItem>
        {/* The way back to the legacy calendar, demoted from a prominent button into this menu now
            that the new page is trusted (see LegacyMenuItem / NextMigration.legacyListInMenu). */}
        <DropdownMenuSeparator />
        <LegacyMenuItem url="react/calendar?legacyEscape" />
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
