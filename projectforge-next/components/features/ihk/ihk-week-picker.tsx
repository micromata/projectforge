"use client";

import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import { DateInput } from "@/components/shared/date-input";
import { PeriodStepper } from "@/components/shared/period-stepper";
import { useFormatContext } from "@/hooks/use-format";
import { periodKindOf, type PeriodKind } from "@/lib/date-period";
import { endOfWeek, firstOfWeek } from "@/lib/date-period-math";
import { formatDateRange } from "@/lib/format";

/** Monday: the IHK reports are German and always run Monday to Sunday, whatever the user's first day of week. */
const MONDAY = 1;

/** The calendar week with its begin pinned to Monday, ignoring `ctx.weekStartsOn`. */
export const IHK_WEEK: PeriodKind = {
  ...periodKindOf("week")!,
  beginOf: (iso) => firstOfWeek(iso, MONDAY),
  endOf: (iso) => endOfWeek(iso, MONDAY),
  shift: (iso, steps) => firstOfWeek(iso, MONDAY, steps),
};

/** The Monday of the week `iso` lies in. */
export function mondayOf(iso: string): string {
  return firstOfWeek(iso, MONDAY);
}

/**
 * Picks the report week: any day typed or picked selects its Monday-to-Sunday week, the stepper pages
 * week by week (Wicket's QuickSelectWeekPanel).
 */
export function IhkWeekPicker({
  monday,
  onChange,
}: {
  monday: string;
  onChange: (monday: string) => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor="ihk-week">{t("plugins.ihk.week")}</Label>
      <div className="flex flex-wrap items-center gap-3">
        <DateInput
          id="ihk-week"
          className="w-40"
          value={monday}
          onChange={(value) => value && onChange(mondayOf(value))}
        />
        <PeriodStepper
          currentButton
          kinds={[IHK_WEEK]}
          current={{ kind: IHK_WEEK, anchor: monday }}
          onSelect={(_, anchor) => onChange(mondayOf(anchor))}
        />
        <span className="text-sm text-muted-foreground tabular-nums">
          {formatDateRange(monday, endOfWeek(monday, MONDAY), ctx)}
        </span>
      </div>
    </div>
  );
}
