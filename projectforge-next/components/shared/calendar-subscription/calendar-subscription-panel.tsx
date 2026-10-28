"use client";

import { useId, useState } from "react";
import { useTranslations } from "next-intl";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { CopyableValue } from "@/components/shared/copyable-value";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { CalendarSubscriptionInfo } from "@/lib/rs/calendar-subscription";

/**
 * The subscription link of a calendar feed for the user to add to their calendar app: the url to copy, a QR
 * code for a phone, and the security advice — the url carries the user's personal login token (as the legacy
 * `AbstractICSExportDialog`).
 *
 * A team calendar's feed comes in two variants, with and without the reminders of its events
 * (`urlWithoutExportedReminders`); then a checkbox switches between them, starting at the backend's default.
 */
export function CalendarSubscriptionPanel({
  info,
}: {
  info: CalendarSubscriptionInfo | undefined;
}) {
  const t = useTranslations();
  const checkboxId = useId();
  const [withReminders, setWithReminders] = useState(
    info?.remindersExportDefaultValue ?? true
  );
  const withoutReminders = info?.urlWithoutExportedReminders;
  const useWithoutReminders = !!withoutReminders && !withReminders;
  const url = useWithoutReminders ? withoutReminders : info?.url;
  const qrCode = useWithoutReminders
    ? info?.qrCodeWithoutExportedReminders
    : info?.qrCode;
  const label = info?.headline ?? t("plugins.teamcal.subscription._");

  return (
    <div className="flex flex-col gap-4">
      {withoutReminders && (
        <HintTooltip
          text={t("plugins.teamcal.export.reminder.checkbox.tooltip")}
        >
          <div className="flex items-center gap-2 self-start">
            <Checkbox
              id={checkboxId}
              checked={withReminders}
              onCheckedChange={(checked) => setWithReminders(checked === true)}
            />
            <Label htmlFor={checkboxId} className="font-normal">
              {t("plugins.teamcal.export.reminder.checkbox._")}
            </Label>
          </div>
        </HintTooltip>
      )}
      <CopyableValue value={url} label={label} />
      {url && qrCode && (
        // eslint-disable-next-line @next/next/no-img-element -- rendered by the backend at runtime.
        <img
          src={qrCode}
          alt={info?.headline ?? ""}
          className="size-48 self-center"
        />
      )}
      {info?.securityAdvise && (
        <Alert variant="destructive">
          <AlertTitle>{info.securityAdviseHeadline}</AlertTitle>
          <AlertDescription>{info.securityAdvise}</AlertDescription>
        </Alert>
      )}
    </div>
  );
}
