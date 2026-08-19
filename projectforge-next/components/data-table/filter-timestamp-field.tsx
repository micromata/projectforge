"use client";

import { useTranslations } from "next-intl";
import { DateTimeInput } from "@/components/shared/date-time-input";
import { PeriodStepper } from "@/components/shared/period-stepper";
import { RangeBounds } from "@/components/shared/range-bounds";
import { useFormatContext } from "@/hooks/use-format";
import { periodUnitsOf, type PeriodUnitId } from "@/lib/date-period";
import {
  anchorOfInstantBounds,
  instantBoundsOfPeriod,
  periodOfInstantBounds,
} from "@/lib/date-period-instant";
import { DEFAULT_TO_TIME } from "@/lib/user-zone";
import type { FilterElement, MagicFilterEntryValue } from "@/lib/rs/types";
import type { FilterInputProps } from "./filter-field-inputs";
import { IntervalPresetsSelect } from "./filter-interval-presets-select";

/**
 * A TIMESTAMP filter (org.projectforge.ui.filter.UIFilterTimestampElement): a range of instants,
 * either bound optional (`openInterval`).
 *
 * Both bounds carry a time of day, which a DATE filter's two date inputs cannot express — and must:
 * `PFDateTimeUtils.parseAndCreateDateTime` parses a timestamp field with
 * `parseLocalDateIfNoTimeOfDayGiven = false`, so a bare `2026-07-15` yields null and the bound is
 * silently dropped.
 *
 * Two kinds of quick access, and they are not the same thing: the [PeriodStepper] pages whole
 * calendar periods, while the rolling windows of [IntervalPresetsSelect] always end at now. The
 * latter appear only when the backend marks the field `UNTIL_NOW`, which for the history filter it
 * does.
 */
export function TimestampRangeField({
  element,
  value,
  onChange,
  label,
  onSubmit,
  /** As in [RangeField]; a whole month becomes 00:00 of its first day until 23:59 of its last. */
  periodUnits = ["month"],
}: FilterInputProps & {
  element: FilterElement;
  periodUnits?: PeriodUnitId[];
}) {
  const t = useTranslations("filter");
  const ctx = useFormatContext();
  const units = periodUnitsOf(periodUnits);
  const showPresets = element.selectors?.includes("UNTIL_NOW") ?? false;

  function next(
    part: "from" | "to",
    iso: string | null
  ): MagicFilterEntryValue | undefined {
    const merged = { ...value, [part]: iso ?? undefined };
    return merged.from || merged.to ? merged : undefined;
  }

  return (
    <div className="space-y-1.5">
      <p className="text-xs font-medium">{label}</p>
      {/* A date plus a time per bound, so the two only fit next to each other in a wide column. */}
      <RangeBounds breakpoint="@xl">
        <DateTimeInput
          // As in [RangeField]: the first bound is a date field, and focusing one opens its calendar
          // over the rest of the pill.
          autoFocus={false}
          dateLabel={`${label}: ${t("dateFrom")}`}
          timeLabel={`${label}: ${t("timeFrom")}`}
          value={value?.from}
          onChange={(iso) => onChange(next("from", iso))}
          // The instant the input just committed — `value` here is still the previous one.
          onSubmit={(iso) => onSubmit?.(next("from", iso))}
        />
        <DateTimeInput
          dateLabel={`${label}: ${t("dateTo")}`}
          timeLabel={`${label}: ${t("timeTo")}`}
          // A date typed as the end of a range means the whole day, not its midnight.
          fallbackTime={DEFAULT_TO_TIME}
          value={value?.to}
          onChange={(iso) => onChange(next("to", iso))}
          onSubmit={(iso) => onSubmit?.(next("to", iso))}
        />
      </RangeBounds>
      <PeriodStepper
        units={units}
        current={periodOfInstantBounds(value?.from, value?.to, units, ctx)}
        // As in [RangeField]: the month named is the one the bounds on screen lie in.
        anchor={anchorOfInstantBounds(units[0], value?.from, value?.to, ctx)}
        onSelect={(unit, anchor) => {
          const bounds = instantBoundsOfPeriod(unit, anchor, ctx);
          if (bounds) onChange(bounds);
        }}
      />
      {showPresets && (
        <IntervalPresetsSelect
          onChange={onChange}
          className="mt-0.5 w-full text-xs"
        />
      )}
    </div>
  );
}
