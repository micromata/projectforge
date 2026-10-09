"use client";

import { useTranslations } from "next-intl";
import { NumberField } from "@/components/shared/form/number-field";
import { SelectField } from "@/components/shared/form/select-field";
import { cn } from "@/lib/utils";
import { useRuleValue } from "./use-rule-value";

/** Each weekday spelt out so the i18n key scanner sees every key (see NextI18nKeyScanner). */
const WEEKDAYS = [
  { value: "MONDAY", key: "calendar.day.monday" },
  { value: "TUESDAY", key: "calendar.day.tuesday" },
  { value: "WEDNESDAY", key: "calendar.day.wednesday" },
  { value: "THURSDAY", key: "calendar.day.thursday" },
  { value: "FRIDAY", key: "calendar.day.friday" },
  { value: "SATURDAY", key: "calendar.day.saturday" },
  { value: "SUNDAY", key: "calendar.day.sunday" },
] as const;

/**
 * When the rule runs by itself (NotificationSchedule): the mode, and the day or weekday it needs.
 * The rules are checked once a day in the morning, so a day is all there is to choose.
 */
export function ScheduleField({ className }: { className?: string }) {
  const t = useTranslations();
  const mode = useRuleValue("scheduleMode");
  const modes = [
    { value: "NONE", label: t("notification.schedule.mode.NONE") },
    {
      value: "WORKING_DAYS_BEFORE_MONTH_END",
      label: t("notification.schedule.mode.WORKING_DAYS_BEFORE_MONTH_END"),
    },
    {
      value: "DAY_OF_MONTH",
      label: t("notification.schedule.mode.DAY_OF_MONTH"),
    },
    { value: "DAILY", label: t("notification.schedule.mode.DAILY") },
    { value: "WEEKLY", label: t("notification.schedule.mode.WEEKLY") },
  ];
  return (
    <div className={cn("grid gap-4 sm:grid-cols-2", className)}>
      <SelectField
        name="scheduleMode"
        label={t("notification.rule.schedule")}
        hint={t("notification.schedule.info")}
        metadataLess
        clearable={false}
        options={modes}
      />
      {(mode === "WORKING_DAYS_BEFORE_MONTH_END" ||
        mode === "DAY_OF_MONTH") && (
        <NumberField
          name="scheduleDay"
          label={t("notification.schedule.day")}
          hint={
            mode === "WORKING_DAYS_BEFORE_MONTH_END"
              ? t("notification.schedule.workingDaysInfo")
              : undefined
          }
          metadataLess
          fractionDigits={0}
          maxDigits={2}
        />
      )}
      {mode === "WEEKLY" && (
        <SelectField
          name="scheduleDayOfWeek"
          label={t("notification.schedule.dayOfWeek")}
          metadataLess
          clearable={false}
          options={WEEKDAYS.map((day) => ({
            value: day.value,
            label: t(day.key),
          }))}
        />
      )}
    </div>
  );
}
