"use client";

import { useTranslations } from "next-intl";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { OnTheDay, RecurrenceModel, SetPos } from "../recurrence-model";

/**
 * The "which" and "day" of an "on the …" rule, spelt out in full so the i18n key scanner sees each key (a
 * `t(\`…${value}\`)` built at runtime would be invisible to it, see NextI18nKeyScanner). The day list adds
 * the three groups (Day / Weekday / Weekend day) the legacy dropdown offers ahead of the seven weekdays.
 */
const WHICH_OPTIONS: { value: SetPos; labelKey: string }[] = [
  { value: 1, labelKey: "plugins.teamcal.event.recurrence.first" },
  { value: 2, labelKey: "plugins.teamcal.event.recurrence.second" },
  { value: 3, labelKey: "plugins.teamcal.event.recurrence.third" },
  { value: 4, labelKey: "plugins.teamcal.event.recurrence.fourth" },
  { value: -1, labelKey: "plugins.teamcal.event.recurrence.last" },
];
const DAY_OPTIONS: { value: OnTheDay; labelKey: string }[] = [
  { value: "DAY", labelKey: "plugins.teamcal.event.recurrence.day" },
  { value: "WEEKDAY", labelKey: "plugins.teamcal.event.recurrence.weekday" },
  { value: "WEEKENDDAY", labelKey: "plugins.teamcal.event.recurrence.weekend" },
  { value: "MO", labelKey: "plugins.teamcal.event.recurrence.monday" },
  { value: "TU", labelKey: "plugins.teamcal.event.recurrence.tuesday" },
  { value: "WE", labelKey: "plugins.teamcal.event.recurrence.wednesday" },
  { value: "TH", labelKey: "plugins.teamcal.event.recurrence.thursday" },
  { value: "FR", labelKey: "plugins.teamcal.event.recurrence.friday" },
  { value: "SA", labelKey: "plugins.teamcal.event.recurrence.saturday" },
  { value: "SU", labelKey: "plugins.teamcal.event.recurrence.sunday" },
];

interface Props {
  model: RecurrenceModel;
  update: (next: RecurrenceModel) => void;
  disabled?: boolean;
}

/** The shared "[first] [Monday]" pair of an on-the rule, used by both the monthly and yearly detail. */
export function OnThePicker({ model, update, disabled }: Props) {
  const t = useTranslations();
  return (
    <div className="flex items-center gap-2">
      <Select
        value={String(model.which)}
        disabled={disabled}
        onValueChange={(v) => update({ ...model, which: Number(v) as SetPos })}
      >
        <SelectTrigger
          aria-label={t("plugins.teamcal.event.recurrence.atthe")}
          className="w-full"
        >
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {WHICH_OPTIONS.map((o) => (
            <SelectItem key={o.value} value={String(o.value)}>
              {t(o.labelKey)}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Select
        value={model.onTheDay}
        disabled={disabled}
        onValueChange={(v) => update({ ...model, onTheDay: v as OnTheDay })}
      >
        <SelectTrigger
          aria-label={t("plugins.teamcal.event.recurrence.weekday")}
          className="w-full"
        >
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {DAY_OPTIONS.map((o) => (
            <SelectItem key={o.value} value={o.value}>
              {t(o.labelKey)}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
