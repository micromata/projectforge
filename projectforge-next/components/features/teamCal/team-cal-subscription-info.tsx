"use client";

import { useTranslations } from "next-intl";
import { useEntityData } from "@/components/shared/form/form-context";
import { CalendarSubscriptionPanel } from "@/components/shared/calendar-subscription/calendar-subscription-panel";
import { cn } from "@/lib/utils";
import type { TeamCalDetail } from "./types";

/**
 * The feed of a stored calendar for the user's calendar app (`TeamCal.subscriptionInfo`, sent in edit mode
 * only): the url with or without the events' reminders, its QR code and the security advice.
 */
export function TeamCalSubscriptionInfo({ className }: { className?: string }) {
  const t = useTranslations();
  const info = useEntityData<TeamCalDetail>()?.subscriptionInfo ?? undefined;
  if (!info) return null;
  return (
    <div className={cn("flex flex-col gap-3", className)}>
      <p className="text-sm text-muted-foreground">
        {t("plugins.teamcal.subscription.tooltip")}
      </p>
      {/* Keyed by the url: a reload of the calendar resets the reminder switch to the backend's default. */}
      <CalendarSubscriptionPanel key={info.url} info={info} />
    </div>
  );
}
