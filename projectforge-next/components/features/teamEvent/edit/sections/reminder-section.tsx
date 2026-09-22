"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { cn } from "@/lib/utils";
import type { TeamEventEditValues } from "../team-event-edit-schema";

/** The reminder is off until an action is chosen; then the legacy form presets 15 minutes before. */
const DEFAULT_DURATION = 15;
const DEFAULT_UNIT = "MINUTES";

/** The empty action-type option, since a shadcn `SelectItem` may not carry an empty value. */
const NO_REMINDER = "none";
/**
 * The client's before-notification kinds (`ReminderActionType`) and lead-time units
 * (`ReminderDurationUnit`), each with its label key spelt out in full so the i18n key scanner sees it
 * (a `t(\`…${value}\`)` would be invisible to it, see NextI18nKeyScanner).
 */
const ACTION_TYPES = [
  { value: "MESSAGE", labelKey: "plugins.teamcal.event.reminder.MESSAGE" },
  {
    value: "MESSAGE_SOUND",
    labelKey: "plugins.teamcal.event.reminder.MESSAGE_SOUND",
  },
] as const;
const DURATION_UNITS = [
  {
    value: "MINUTES",
    labelKey: "plugins.teamcal.event.reminder.MINUTES_BEFORE",
  },
  { value: "HOURS", labelKey: "plugins.teamcal.event.reminder.HOURS_BEFORE" },
  { value: "DAYS", labelKey: "plugins.teamcal.event.reminder.DAYS_BEFORE" },
] as const;

/**
 * When to remind before the event — the `reminderActionType`, `reminderDuration` and
 * `reminderDurationUnit` fields, mirroring the legacy `CalendarEventReminder` customized part.
 *
 * Its own component rather than the shared metadata-bound fields, because none of the three is a
 * `TeamEventDO` column the generator carries — they live on the DTO alone (see team-event-edit-schema.ts).
 * The action type drives the other two: on "none" all three are cleared, and on picking an action the
 * legacy 15-minutes-before preset fills the duration and unit if they were empty, so the reminder is
 * never half-set. A reminder ProjectForge does not fire itself — the calendar clients do (see the
 * `plugins.teamcal.event.reminder.tooltip`).
 */
export function ReminderSection({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const ids = useFieldIds();

  const actionType = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as TeamEventEditValues).reminderActionType
  );

  const onActionChange = (value: string) => {
    if (value === NO_REMINDER) {
      form.setFieldValue("reminderActionType", null);
      form.setFieldValue("reminderDuration", null);
      form.setFieldValue("reminderDurationUnit", null);
      return;
    }
    form.setFieldValue("reminderActionType", value);
    const values = form.state.values as TeamEventEditValues;
    if (values.reminderDuration == null)
      form.setFieldValue("reminderDuration", DEFAULT_DURATION);
    if (!values.reminderDurationUnit)
      form.setFieldValue("reminderDurationUnit", DEFAULT_UNIT);
  };

  return (
    <div
      className={cn(
        "grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3",
        className
      )}
    >
      <FieldShell
        name="reminderActionType"
        label={t("plugins.teamcal.event.reminder.title")}
        invalid={false}
        errors={[]}
        ids={ids}
      >
        <Select
          value={actionType ?? NO_REMINDER}
          onValueChange={onActionChange}
        >
          <SelectTrigger
            id={ids.controlId}
            aria-labelledby={ids.labelId}
            className="w-full"
          >
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={NO_REMINDER}>
              {t("plugins.teamcal.event.reminder.NONE")}
            </SelectItem>
            {ACTION_TYPES.map((type) => (
              <SelectItem key={type.value} value={type.value}>
                {t(type.labelKey)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </FieldShell>

      {actionType ? <LeadTimeField /> : null}
    </div>
  );
}

/**
 * How long before the start the reminder fires — a positive count and its unit, in one shell since the
 * two together are the one thing ("15 minutes before") the reminder is set to. Shown only once an action
 * type is picked, as the legacy form did.
 */
function LeadTimeField() {
  const form = useEntityEditForm();
  const t = useTranslations();
  const ids = useFieldIds();
  const label = t("plugins.teamcal.event.reminder.options");
  return (
    <FieldShell
      name="reminderDuration"
      label={label}
      invalid={false}
      errors={[]}
      ids={ids}
      className="md:col-span-2"
    >
      <div className="flex items-center gap-2">
        <form.Field name={"reminderDuration" as never}>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          {(field: any) => {
            const value = field.state.value as number | null;
            return (
              <Input
                id={ids.controlId}
                type="number"
                min={1}
                aria-label={label}
                className="w-24"
                value={value == null ? "" : String(value)}
                onChange={(e) => {
                  const next = e.target.value;
                  field.handleChange(next === "" ? null : Number(next));
                }}
                onBlur={field.handleBlur}
              />
            );
          }}
        </form.Field>
        <form.Field name={"reminderDurationUnit" as never}>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          {(field: any) => {
            const value = field.state.value as string | null;
            return (
              <Select
                value={value ?? DEFAULT_UNIT}
                onValueChange={(v) => field.handleChange(v)}
              >
                <SelectTrigger aria-label={label} className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {DURATION_UNITS.map((unit) => (
                    <SelectItem key={unit.value} value={unit.value}>
                      {t(unit.labelKey)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            );
          }}
        </form.Field>
      </div>
    </FieldShell>
  );
}
