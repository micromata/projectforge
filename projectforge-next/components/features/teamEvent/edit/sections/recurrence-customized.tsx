"use client";

import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { DateInput } from "@/components/shared/date-input";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import {
  WEEKDAYS,
  type RecurrenceFreq,
  type RecurrenceModel,
  type Weekday,
} from "../recurrence-rrule";

/**
 * The frequencies, interval units and weekday labels the customized panel offers, each with its i18n key
 * spelt out in full: a `t(\`…${value}\`)` built at runtime would be invisible to the key scanner (see
 * NextI18nKeyScanner), so every key a control shows is a literal here.
 */
const FREQUENCIES: { value: RecurrenceFreq; labelKey: string }[] = [
  { value: "YEARLY", labelKey: "common.recurrence.frequency.yearly" },
  { value: "MONTHLY", labelKey: "common.recurrence.frequency.monthly" },
  { value: "WEEKLY", labelKey: "common.recurrence.frequency.weekly" },
  { value: "DAILY", labelKey: "common.recurrence.frequency.daily" },
];
/** The "each … day(s)/week(s)/…" unit shown after the interval, per frequency. */
const INTERVAL_UNIT: Record<RecurrenceFreq, string> = {
  DAILY: "plugins.teamcal.event.recurrence.customized.day",
  WEEKLY: "plugins.teamcal.event.recurrence.customized.week",
  MONTHLY: "plugins.teamcal.event.recurrence.customized.month",
  YEARLY: "plugins.teamcal.event.recurrence.customized.year",
};
const WEEKDAY_LABEL: Record<Weekday, string> = {
  MO: "plugins.teamcal.event.recurrence.monday",
  TU: "plugins.teamcal.event.recurrence.tuesday",
  WE: "plugins.teamcal.event.recurrence.wednesday",
  TH: "plugins.teamcal.event.recurrence.thursday",
  FR: "plugins.teamcal.event.recurrence.friday",
  SA: "plugins.teamcal.event.recurrence.saturday",
  SU: "plugins.teamcal.event.recurrence.sunday",
};

interface Props {
  model: RecurrenceModel;
  update: (next: RecurrenceModel) => void;
}

/**
 * The "customized" recurrence panel — the legacy `RRuleGenerator` reduced to the frequency, its interval,
 * the weekdays a weekly rule falls on and the optional end date. Shown only while the section's mode is
 * "customized"; the plain frequencies write a bare `FREQ=…;INTERVAL=1` and show none of this.
 */
export function RecurrenceCustomized({ model, update }: Props) {
  const freq = model.freq ?? "WEEKLY";
  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3">
        <FrequencyField freq={freq} model={model} update={update} />
        <IntervalField freq={freq} model={model} update={update} />
        <UntilField model={model} update={update} />
      </div>
      {freq === "WEEKLY" ? (
        <WeekdayPicker model={model} update={update} />
      ) : null}
    </div>
  );
}

/** Which of the four frequencies the customized rule repeats on. */
function FrequencyField({
  freq,
  model,
  update,
}: Props & { freq: RecurrenceFreq }) {
  const t = useTranslations();
  const ids = useFieldIds();
  const label = t("common.recurrence.frequency.label");
  return (
    <FieldShell name="freq" label={label} invalid={false} errors={[]} ids={ids}>
      <Select
        value={freq}
        onValueChange={(v) => update({ ...model, freq: v as RecurrenceFreq })}
      >
        <SelectTrigger
          id={ids.controlId}
          aria-labelledby={ids.labelId}
          className="w-full"
        >
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {FREQUENCIES.map((f) => (
            <SelectItem key={f.value} value={f.value}>
              {t(f.labelKey)}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </FieldShell>
  );
}

/** How many periods apart the event repeats — "each 2 week(s)". */
function IntervalField({
  freq,
  model,
  update,
}: Props & { freq: RecurrenceFreq }) {
  const t = useTranslations();
  const ids = useFieldIds();
  const label = t("plugins.teamcal.event.recurrence.each");
  return (
    <FieldShell
      name="interval"
      label={label}
      invalid={false}
      errors={[]}
      ids={ids}
    >
      <div className="flex items-center gap-2">
        <Input
          id={ids.controlId}
          type="number"
          min={1}
          aria-label={label}
          className="w-24"
          value={String(model.interval)}
          onChange={(e) => {
            const next = Number(e.target.value);
            update({ ...model, interval: next > 0 ? next : 1 });
          }}
        />
        <span className="text-sm text-muted-foreground">
          {t(INTERVAL_UNIT[freq])}
        </span>
      </div>
    </FieldShell>
  );
}

/** The optional last day the series runs to; empty means it repeats without end. */
function UntilField({ model, update }: Props) {
  const t = useTranslations();
  const ids = useFieldIds();
  const label = t("plugins.teamcal.event.recurrence.until");
  return (
    <FieldShell
      name="recurrenceUntil"
      label={label}
      invalid={false}
      errors={[]}
      ids={ids}
    >
      <DateInput
        id={ids.controlId}
        aria-label={label}
        value={model.until}
        onChange={(until) => update({ ...model, until })}
      />
    </FieldShell>
  );
}

/** Which weekdays a weekly rule falls on — none picked means every day the interval lands on. */
function WeekdayPicker({ model, update }: Props) {
  const t = useTranslations();
  const toggle = (day: Weekday, on: boolean) =>
    update({
      ...model,
      byWeekday: on
        ? WEEKDAYS.filter((d) => d === day || model.byWeekday.includes(d))
        : model.byWeekday.filter((d) => d !== day),
    });
  return (
    <div
      role="group"
      aria-label={t("plugins.teamcal.event.recurrence.weekday")}
      className="flex flex-wrap gap-x-4 gap-y-2"
    >
      {WEEKDAYS.map((day) => (
        <div key={day} className="flex items-center gap-2">
          <Checkbox
            id={`recurrence-day-${day}`}
            checked={model.byWeekday.includes(day)}
            onCheckedChange={(checked) => toggle(day, checked === true)}
          />
          <Label
            htmlFor={`recurrence-day-${day}`}
            className="text-xs font-normal text-foreground"
          >
            {t(WEEKDAY_LABEL[day])}
          </Label>
        </div>
      ))}
    </div>
  );
}
