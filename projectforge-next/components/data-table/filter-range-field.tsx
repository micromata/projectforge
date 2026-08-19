"use client";

import { useTranslations } from "next-intl";
import { DateInput } from "@/components/shared/date-input";
import { PeriodStepper } from "@/components/shared/period-stepper";
import { RangeBounds } from "@/components/shared/range-bounds";
import { useFormatContext } from "@/hooks/use-format";
import {
  anchorOfBounds,
  boundsOfPeriod,
  periodOfBounds,
  periodUnitsOf,
  type PeriodUnitId,
} from "@/lib/date-period";
import type { FilterInputProps } from "./filter-field-inputs";

/**
 * A DATE filter (org.projectforge.ui.filter.UIFilterElement with FilterType.DATE): two `yyyy-MM-dd`
 * bounds, either of them optional, with quick access to a whole period below them.
 *
 * Kept apart from [TimestampRangeField] deliberately: a timestamp bound carries a time of day, and
 * sent without one the backend parses it to null and drops the bound.
 */
export function RangeField({
  value,
  onChange,
  label,
  onSubmit,
  /**
   * Granularities the period stepper offers; `[]` leaves it out. A date filter is asked "which
   * month?" often enough that the month is the default — see [PeriodStepper].
   */
  periodUnits = ["month"],
}: FilterInputProps & { periodUnits?: PeriodUnitId[] }) {
  const t = useTranslations("filter");
  const ctx = useFormatContext();
  const units = periodUnitsOf(periodUnits);

  function next(part: "from" | "to", raw: string | null) {
    const merged = { ...value, [part]: raw ?? undefined };
    return merged.from || merged.to ? merged : undefined;
  }

  return (
    <div className="space-y-1">
      <p className="text-xs font-medium">{label}</p>
      <RangeBounds breakpoint="@2xs">
        <DateInput
          // Never focused on open, whatever the pill asks for: focusing a [DateInput] opens its
          // calendar, and that popover would cover the second bound and the stepper below it. A period
          // filter is opened to page or to pick as often as to type, and the field is one click away.
          autoFocus={false}
          aria-label={`${label}: ${t("value")}`}
          value={value?.from}
          defaultMonth={value?.to}
          onChange={(iso) => onChange(next("from", iso))}
          // The date [DateInput] just committed, since `value` here is still the previous one.
          onSubmit={(iso) => onSubmit?.(next("from", iso))}
        />
        <DateInput
          aria-label={`${label}: ${t("valueTo")}`}
          value={value?.to}
          // Opens in the month of the range's start while the end is still empty.
          defaultMonth={value?.from}
          onChange={(iso) => onChange(next("to", iso))}
          onSubmit={(iso) => onSubmit?.(next("to", iso))}
        />
      </RangeBounds>
      <PeriodStepper
        units={units}
        current={periodOfBounds(value?.from, value?.to, units, ctx)}
        // Follows the dates on screen: a start date typed by hand decides which month the arrows page
        // from, so one click cannot jump somewhere the user never named.
        anchor={anchorOfBounds(units[0], value?.from, value?.to, ctx)}
        // No `onSubmit`: paging must not close the pill it sits in (see [PeriodStepper]).
        onSelect={(unit, anchor) => onChange(boundsOfPeriod(unit, anchor, ctx))}
      />
    </div>
  );
}
