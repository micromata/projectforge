"use client";

import { useTranslations } from "next-intl";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { cn } from "@/lib/utils";
import {
  WEEKDAYS,
  type RecurrenceModel,
  type Weekday,
} from "../recurrence-model";

/** Each weekday spelt out in full so the i18n key scanner sees every key (see NextI18nKeyScanner). */
const WEEKDAY_KEYS: Record<Weekday, string> = {
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
 * The weekly Mo–So toggle row (`BYDAY`). A multiple toggle group keeps the picked weekdays in the model's
 * canonical Monday-first order, so the serialized rule is stable regardless of the click order.
 */
export function RecurrenceWeekdayToggle({ model, update }: Props) {
  const t = useTranslations();
  return (
    <ToggleGroup
      type="multiple"
      variant="outline"
      value={model.weeklyDays}
      onValueChange={(days) =>
        update({
          ...model,
          weeklyDays: WEEKDAYS.filter((d) => days.includes(d)),
        })
      }
      className="flex-wrap justify-start"
    >
      {WEEKDAYS.map((day) => (
        <ToggleGroupItem
          key={day}
          value={day}
          aria-label={t(WEEKDAY_KEYS[day])}
          // The default toggle-on state (a faint `bg-muted`) reads as barely selected next to the
          // outlined off state; a filled primary chip makes the picked weekdays unmistakable.
          className={cn(
            "data-[state=on]:border-primary data-[state=on]:bg-primary data-[state=on]:text-primary-foreground",
            "hover:data-[state=on]:bg-primary/90"
          )}
        >
          {t(WEEKDAY_KEYS[day])}
        </ToggleGroupItem>
      ))}
    </ToggleGroup>
  );
}
