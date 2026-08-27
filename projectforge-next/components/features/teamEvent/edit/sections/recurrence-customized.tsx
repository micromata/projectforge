"use client";

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
import type { RecurrenceFreq, RecurrenceModel } from "../recurrence-model";
import { RecurrenceDetailMonthly } from "./recurrence-detail-monthly";
import { RecurrenceDetailYearly } from "./recurrence-detail-yearly";
import { RecurrenceEndField } from "./recurrence-end-field";
import { RecurrenceWeekdayToggle } from "./recurrence-weekday-toggle";

/**
 * The frequencies and interval units the customized panel offers, each i18n key spelt out in full: a
 * `t(\`…${value}\`)` built at runtime would be invisible to the key scanner (see NextI18nKeyScanner).
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

interface Props {
  model: RecurrenceModel;
  update: (next: RecurrenceModel) => void;
}

/**
 * The "customized" recurrence panel — a faithful replica of the legacy `RRuleGenerator`: a frequency
 * sub-select, the interval (monthly/weekly/daily; yearly repeats every year), the per-frequency detail
 * (yearly/monthly on vs. on-the, the weekly weekday toggles) and the shared end row. Shown only while the
 * section's mode is "customized"; the plain frequencies write a bare `FREQ=…;INTERVAL=1` and show none of
 * this. Each row delegates to its own component so this stays a thin orchestrator.
 */
export function RecurrenceCustomized({ model, update }: Props) {
  const freq = model.freq ?? "WEEKLY";
  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-2">
        <FrequencyField freq={freq} model={model} update={update} />
        {freq !== "YEARLY" ? (
          <IntervalField freq={freq} model={model} update={update} />
        ) : null}
      </div>
      {freq === "YEARLY" ? (
        <RecurrenceDetailYearly model={model} update={update} />
      ) : null}
      {freq === "MONTHLY" ? (
        <RecurrenceDetailMonthly model={model} update={update} />
      ) : null}
      {freq === "WEEKLY" ? (
        <RecurrenceWeekdayToggle model={model} update={update} />
      ) : null}
      <RecurrenceEndField model={model} update={update} />
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
