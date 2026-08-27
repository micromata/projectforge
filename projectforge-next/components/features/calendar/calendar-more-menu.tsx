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
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
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
      <DropdownMenuContent align="end">
        <DropdownMenuItem onSelect={onRefresh}>
          <HugeiconsIcon icon={CircleArrowReload01Icon} size={14} />
          {t("reload")}
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <a href={teamCalList}>
            <HugeiconsIcon icon={Calendar03Icon} size={14} />
            {t("menu.plugins.teamcal")}
          </a>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
