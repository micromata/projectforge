"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { RecurrenceModel } from "../recurrence-model";
import { OnThePicker } from "./recurrence-onthe-picker";

/** The twelve months, each key spelt out so the scanner sees it (see NextI18nKeyScanner). Index+1 = month. */
const MONTH_KEYS = [
  "calendar.month.january",
  "calendar.month.february",
  "calendar.month.march",
  "calendar.month.april",
  "calendar.month.may",
  "calendar.month.june",
  "calendar.month.july",
  "calendar.month.august",
  "calendar.month.september",
  "calendar.month.october",
  "calendar.month.november",
  "calendar.month.december",
];

interface Props {
  model: RecurrenceModel;
  update: (next: RecurrenceModel) => void;
}

/**
 * The yearly detail: either "on [month] [day]" (`BYMONTH`+`BYMONTHDAY`) or "on the [first] [Monday] in
 * [month]" (`BYSETPOS`+`BYDAY`+`BYMONTH`), the two rows a radio switches between. The inactive row's
 * controls are disabled, as the legacy generator greyed them out.
 */
export function RecurrenceDetailYearly({ model, update }: Props) {
  const t = useTranslations();
  const on = model.yearlyMode === "ON";
  return (
    <RadioGroup
      value={model.yearlyMode}
      onValueChange={(v) =>
        update({ ...model, yearlyMode: v as RecurrenceModel["yearlyMode"] })
      }
      className="gap-3"
    >
      <div className="flex flex-wrap items-center gap-2">
        <RadioGroupItem id="yearly-on" value="ON" />
        <Label htmlFor="yearly-on" className="font-normal">
          {t("plugins.teamcal.event.recurrence.on")}
        </Label>
        <MonthSelect model={model} update={update} disabled={!on} />
        <Input
          type="number"
          min={1}
          max={31}
          disabled={!on}
          aria-label={t("plugins.teamcal.event.recurrence.day")}
          className="w-20"
          value={String(model.yearlyDay)}
          onChange={(e) =>
            update({ ...model, yearlyDay: clampDay(e.target.value) })
          }
        />
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <RadioGroupItem id="yearly-onthe" value="ONTHE" />
        <Label htmlFor="yearly-onthe" className="font-normal">
          {t("plugins.teamcal.event.recurrence.onThe")}
        </Label>
        <OnThePicker model={model} update={update} disabled={on} />
        <span className="text-sm text-muted-foreground">
          {t("plugins.teamcal.event.recurrence.in")}
        </span>
        <MonthSelect model={model} update={update} disabled={on} />
      </div>
    </RadioGroup>
  );
}

/** The month of a yearly rule (both rows share `yearlyMonth`). */
function MonthSelect({
  model,
  update,
  disabled,
}: Props & { disabled?: boolean }) {
  const t = useTranslations();
  return (
    <Select
      value={String(model.yearlyMonth)}
      disabled={disabled}
      onValueChange={(v) => update({ ...model, yearlyMonth: Number(v) })}
    >
      <SelectTrigger
        aria-label={t("calendar.month._")}
        className="w-full min-w-32"
      >
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {MONTH_KEYS.map((key, index) => (
          <SelectItem key={key} value={String(index + 1)}>
            {t(key)}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

/** A day-of-month kept in 1..31 so a stray value cannot leave the rule out of range. */
function clampDay(value: string): number {
  const n = Number(value);
  if (!Number.isFinite(n)) return 1;
  return Math.min(Math.max(Math.trunc(n), 1), 31);
}
