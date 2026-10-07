"use client";

import { useState } from "react";
import { GuardedLink } from "@/components/shared/guarded-link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Calendar03Icon,
  CalendarUpload01Icon,
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
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { CalendarSubscriptionDialog } from "@/components/shared/calendar-subscription/calendar-subscription-dialog";
import type { CalendarSubscriptionType } from "@/lib/rs/calendar-subscription";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import { CalendarSubscriptionSubmenu } from "./calendar-subscription-submenu";

interface CalendarMoreMenuProps {
  onRefresh: () => void;
  /** Preselected as the target of the ICS import. */
  defaultCalendarId?: number | null;
}

/**
 * The overflow menu: a manual refresh in place of the legacy page reload, and the list of team
 * calendars — still a legacy page (`teamCal` is not migrated), so a plain anchor that leaves the app —
 * the ICS import, into the default calendar unless another one is picked there, and the subscription
 * links of ProjectForge's own feeds.
 * The colour settings now live in the gear dialog (CalendarColorSettings), not on a separate page.
 */
export function CalendarMoreMenu({
  onRefresh,
  defaultCalendarId,
}: CalendarMoreMenuProps) {
  const t = useTranslations();
  const [subscription, setSubscription] =
    useState<CalendarSubscriptionType | null>(null);
  const teamCalList = toAbsoluteUrl(resolveMenuUrl("react/teamCal"));
  const subscriptionDescription = (type: CalendarSubscriptionType) =>
    type === "HOLIDAYS"
      ? t("plugins.teamcal.export.holidays.tooltip")
      : type === "WEEK_OF_YEAR"
        ? t("plugins.teamcal.export.weekOfYears.tooltip")
        : t("plugins.teamcal.subscription.timesheets");
  const importUrl =
    defaultCalendarId != null
      ? `/teamCalImport?teamCalId=${defaultCalendarId}`
      : "/teamCalImport";

  return (
    <>
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
          <HintTooltip
            side="left"
            text={t("plugins.teamcal.import.ics.tooltip")}
          >
            <DropdownMenuItem asChild>
              <GuardedLink href={importUrl}>
                <HugeiconsIcon icon={CalendarUpload01Icon} size={14} />
                {t("plugins.teamcal.import.ics.title")}
              </GuardedLink>
            </DropdownMenuItem>
          </HintTooltip>
          <CalendarSubscriptionSubmenu onPick={setSubscription} />
        </DropdownMenuContent>
      </DropdownMenu>
      {subscription && (
        <CalendarSubscriptionDialog
          type={subscription}
          description={subscriptionDescription(subscription)}
          onClose={() => setSubscription(null)}
        />
      )}
    </>
  );
}
