"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { ExportMenu, ExportMenuItem } from "@/components/shared/export-menu";
import { CalendarSubscriptionDialog } from "@/components/shared/calendar-subscription/calendar-subscription-dialog";
import type { CalendarSubscriptionType } from "@/lib/rs/calendar-subscription";

/**
 * The export menu of the calendar list: ProjectForge's own feeds (the user's time sheets, the weeks of year,
 * the holidays) to subscribe in a calendar app, as the React list's export menu offered them. Each opens the
 * subscription dialog; the feeds of a team calendar are on its form.
 */
export function TeamCalListActions() {
  const t = useTranslations();
  const [subscription, setSubscription] =
    useState<CalendarSubscriptionType | null>(null);
  // Literal keys, so the i18n generator finds them (it exports no computed ones).
  const feeds: {
    type: CalendarSubscriptionType;
    label: string;
    description: string;
  }[] = [
    {
      type: "TIMESHEETS",
      label: t("plugins.teamcal.export.timesheets"),
      description: t("plugins.teamcal.subscription.timesheets"),
    },
    {
      type: "WEEK_OF_YEAR",
      label: t("plugins.teamcal.export.weekOfYears._"),
      description: t("plugins.teamcal.export.weekOfYears.tooltip"),
    },
    {
      type: "HOLIDAYS",
      label: t("plugins.teamcal.export.holidays._"),
      description: t("plugins.teamcal.export.holidays.tooltip"),
    },
  ];
  const picked = feeds.find((feed) => feed.type === subscription);

  return (
    <>
      <ExportMenu>
        {feeds.map((feed) => (
          <ExportMenuItem
            key={feed.type}
            label={feed.label}
            description={feed.description}
            onSelect={() => setSubscription(feed.type)}
          />
        ))}
      </ExportMenu>
      {picked && (
        <CalendarSubscriptionDialog
          type={picked.type}
          description={picked.description}
          onClose={() => setSubscription(null)}
        />
      )}
    </>
  );
}
