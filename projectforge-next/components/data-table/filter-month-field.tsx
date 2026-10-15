"use client";

import { useTranslations } from "next-intl";
import { MonthInput } from "@/components/shared/month-input";
import { PeriodStepper } from "@/components/shared/period-stepper";
import { RangeBounds } from "@/components/shared/range-bounds";
import { useFormatContext } from "@/hooks/use-format";
import { CUSTOM_PERIOD_KIND } from "@/lib/date-period";
import { anchorOfBounds, boundsOfPeriod } from "@/lib/date-period-bounds";
import type { FilterInputProps } from "./filter-field-inputs";
import { editedDateValue, periodOfDateValue } from "./filter-period";
import { useFilterPeriodKinds } from "./filter-period-kinds";

/**
 * A MONTH filter (org.projectforge.ui.filter.UIFilterElement with FilterType.MONTH): a range of whole
 * calendar months, for a value the backend reads by year and month only — the booking period of an
 * accounting record. Wicket's year/month selects, as two month pickers.
 *
 * The bounds travel as a DATE filter's do (`yyyy-MM-dd`, the first day of the begin month and the last of
 * the end month), so the period arithmetic and the stepper below are [RangeField]'s: every art a list
 * offers begins on the first of a month and ends on the last of one once its anchor does.
 */
export function MonthRangeField({ value, onChange, label }: FilterInputProps) {
  const t = useTranslations("filter");
  const ctx = useFormatContext();
  const kinds = useFilterPeriodKinds();

  function next(part: "from" | "to", iso: string | null) {
    // As in [RangeField]: a picked begin keeps the art and drags the end along it, a picked end dissolves it.
    return editedDateValue(value, part, iso, kinds, ctx);
  }

  return (
    <div className="space-y-1">
      <p className="text-xs font-medium">{label}</p>
      <RangeBounds breakpoint="@2xs">
        <MonthInput
          bound="begin"
          aria-label={`${label}: ${t("monthFrom")}`}
          value={value?.from}
          defaultMonth={value?.to}
          onChange={(iso) => onChange(next("from", iso))}
        />
        <MonthInput
          bound="end"
          aria-label={`${label}: ${t("monthTo")}`}
          value={value?.to}
          defaultMonth={value?.from}
          onChange={(iso) => onChange(next("to", iso))}
        />
      </RangeBounds>
      <PeriodStepper
        kinds={kinds}
        current={periodOfDateValue(value, kinds, ctx)}
        anchor={anchorOfBounds(kinds[0], value?.from, value?.to, ctx)}
        // No `onSubmit`, as in [RangeField]: paging must not close the pill it sits in.
        onSelect={(kind, anchor) =>
          onChange({
            ...boundsOfPeriod(kind, anchor, ctx),
            periodKind: kind.id,
          })
        }
        onClear={() => onChange({ ...value, periodKind: CUSTOM_PERIOD_KIND })}
        longLabel
      />
    </div>
  );
}
