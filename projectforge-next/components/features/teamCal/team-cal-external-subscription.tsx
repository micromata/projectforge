"use client";

import { useEffect } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { InputField } from "@/components/shared/form/input-field";
import { SelectField } from "@/components/shared/form/select-field";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { cn } from "@/lib/utils";
import type { TeamCalValues } from "./team-cal-schema";

/** `SubscriptionUpdateInterval`, in seconds. */
const FIFTEEN_MINUTES = 15 * 60;

/**
 * The external subscription of a calendar (an ics feed of Google, iCloud, …, read-only here): the switch, and
 * once it is on, the url of the feed and how often it is fetched. A calendar switched on gets the shortest
 * interval, the first of the legacy form's choices.
 */
export function TeamCalExternalSubscription({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => state.values as TeamCalValues
  );
  const enabled = values.externalSubscription;
  const interval = values.externalSubscriptionUpdateInterval;
  useEffect(() => {
    if (enabled && interval == null) {
      form.setFieldValue("externalSubscriptionUpdateInterval", FIFTEEN_MINUTES);
    }
  }, [enabled, interval, form]);
  // Literal keys, so the i18n generator finds them (it exports no computed ones).
  const intervals = [
    {
      value: String(FIFTEEN_MINUTES),
      label: t(
        "plugins.teamcal.externalsubscription.updateInterval.interval15Min"
      ),
    },
    {
      value: String(60 * 60),
      label: t(
        "plugins.teamcal.externalsubscription.updateInterval.interval1h"
      ),
    },
    {
      value: String(24 * 60 * 60),
      label: t(
        "plugins.teamcal.externalsubscription.updateInterval.interval1d"
      ),
    },
  ];

  return (
    <div className={cn("flex flex-col gap-4", className)}>
      <CheckboxField
        name="externalSubscription"
        label={t("plugins.teamcal.externalsubscription.label._")}
        hint={t("plugins.teamcal.externalsubscription.label.tooltip")}
      />
      {enabled && (
        <div className="grid gap-4 sm:grid-cols-3">
          <InputField
            name="externalSubscriptionUrl"
            label={t("plugins.teamcal.externalsubscription.url._")}
            hint={t("plugins.teamcal.externalsubscription.url.tooltip")}
            className="sm:col-span-2"
          />
          <SelectField
            name="externalSubscriptionUpdateInterval"
            label={t("plugins.teamcal.externalsubscription.updateInterval._")}
            options={intervals}
            valueType="number"
            clearable={false}
          />
        </div>
      )}
    </div>
  );
}
