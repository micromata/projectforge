"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import type { RecurrenceModel } from "../recurrence-model";
import { OnThePicker } from "./recurrence-onthe-picker";

interface Props {
  model: RecurrenceModel;
  update: (next: RecurrenceModel) => void;
}

/**
 * The monthly detail: either "on day [n]" (`BYMONTHDAY`) or "on the [first] [Monday]"
 * (`BYSETPOS`+`BYDAY`), the two rows a radio switches between. The inactive row's controls are disabled,
 * as the legacy generator greyed them out.
 */
export function RecurrenceDetailMonthly({ model, update }: Props) {
  const t = useTranslations();
  const on = model.monthlyMode === "ON";
  return (
    <RadioGroup
      value={model.monthlyMode}
      onValueChange={(v) =>
        update({ ...model, monthlyMode: v as RecurrenceModel["monthlyMode"] })
      }
      className="gap-3"
    >
      <div className="flex flex-wrap items-center gap-2">
        <RadioGroupItem id="monthly-on" value="ON" />
        <Label htmlFor="monthly-on" className="font-normal">
          {t("plugins.teamcal.event.recurrence.onDay")}
        </Label>
        <Input
          type="number"
          min={1}
          max={31}
          disabled={!on}
          aria-label={t("plugins.teamcal.event.recurrence.onDay")}
          className="w-20"
          value={String(model.monthlyDay)}
          onChange={(e) =>
            update({ ...model, monthlyDay: clampDay(e.target.value) })
          }
        />
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <RadioGroupItem id="monthly-onthe" value="ONTHE" />
        <Label htmlFor="monthly-onthe" className="font-normal">
          {t("plugins.teamcal.event.recurrence.onThe")}
        </Label>
        <OnThePicker model={model} update={update} disabled={on} />
      </div>
    </RadioGroup>
  );
}

/** A day-of-month kept in 1..31 so a stray value cannot leave the rule out of range. */
function clampDay(value: string): number {
  const n = Number(value);
  if (!Number.isFinite(n)) return 1;
  return Math.min(Math.max(Math.trunc(n), 1), 31);
}
