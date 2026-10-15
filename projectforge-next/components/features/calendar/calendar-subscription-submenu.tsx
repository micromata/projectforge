"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { RssIcon } from "@hugeicons/core-free-icons";
import {
  DropdownMenuItem,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
} from "@/components/ui/dropdown-menu";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { CalendarSubscriptionType } from "@/lib/rs/calendar-subscription";

/**
 * The "subscription" submenu of the calendar's overflow menu: ProjectForge's own feeds (time sheets,
 * holidays, weeks of year), as the legacy calendar's subscription menu. Picking one hands its type to
 * [onPick]; the dialog is rendered by the menu's owner, as the dropdown unmounts its items on close.
 */
export function CalendarSubscriptionSubmenu({
  onPick,
}: {
  onPick: (type: CalendarSubscriptionType) => void;
}) {
  const t = useTranslations();
  // Literal keys, so the i18n generator finds them (it exports no computed ones).
  const feeds: {
    type: CalendarSubscriptionType;
    label: string;
    tooltip?: string;
  }[] = [
    { type: "TIMESHEETS", label: t("plugins.teamcal.export.timesheets") },
    {
      type: "HOLIDAYS",
      label: t("plugins.teamcal.export.holidays._"),
      tooltip: t("plugins.teamcal.export.holidays.tooltip"),
    },
    {
      type: "WEEK_OF_YEAR",
      label: t("plugins.teamcal.export.weekOfYears._"),
      tooltip: t("plugins.teamcal.export.weekOfYears.tooltip"),
    },
  ];
  return (
    <DropdownMenuSub>
      <DropdownMenuSubTrigger>
        <HugeiconsIcon icon={RssIcon} size={14} />
        {t("plugins.teamcal.subscription._")}
      </DropdownMenuSubTrigger>
      <DropdownMenuSubContent className="w-64">
        {feeds.map((feed) => (
          <HintTooltip key={feed.type} side="left" text={feed.tooltip}>
            <DropdownMenuItem onSelect={() => onPick(feed.type)}>
              {feed.label}
            </DropdownMenuItem>
          </HintTooltip>
        ))}
      </DropdownMenuSubContent>
    </DropdownMenuSub>
  );
}
