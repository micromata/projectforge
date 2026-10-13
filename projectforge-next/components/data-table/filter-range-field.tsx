"use client";

import { useTranslations } from "next-intl";
import { DateInput } from "@/components/shared/date-input";
import { PeriodStepper } from "@/components/shared/period-stepper";
import { RangeBounds } from "@/components/shared/range-bounds";
import { useFormatContext } from "@/hooks/use-format";
import { CUSTOM_PERIOD_KIND } from "@/lib/date-period";
import { todayOf } from "@/lib/user-zone";
import { anchorOfBounds, boundsOfPeriod } from "@/lib/date-period-bounds";
import type { FilterInputProps } from "./filter-field-inputs";
import { periodOfDateValue } from "./filter-period";
import {
  useFilterDefaultPeriodKind,
  useFilterPeriodKinds,
} from "./filter-period-kinds";
import { usePeriodDefaultSnap } from "./use-period-default-snap";

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
}: FilterInputProps) {
  const t = useTranslations("filter");
  const ctx = useFormatContext();
  // The arts of this list, not of this field: which of them make sense is a property of the page's dates
  // (see [FilterPeriodKindsProvider]).
  const kinds = useFilterPeriodKinds();
  const defaultKind = useFilterDefaultPeriodKind();

  // A date typed snaps to the list's default art (the calendar year of the vacations) once it is typed.
  const snap = usePeriodDefaultSnap(value, kinds, ctx, defaultKind, onChange);

  return (
    <div className="space-y-1">
      <p className="text-xs font-medium">{label}</p>
      <RangeBounds breakpoint="@2xs">
        <DateInput
          key={`from-${snap.revision}`}
          // Never focused on open, whatever the pill asks for: focusing a [DateInput] opens its
          // calendar, and that popover would cover the second bound and the stepper below it. A period
          // filter is opened to page or to pick as often as to type, and the field is one click away.
          autoFocus={false}
          aria-label={`${label}: ${t("value")}`}
          value={value?.from}
          defaultMonth={value?.to}
          onChange={(iso) => snap.change("from", iso)}
          // The date [DateInput] just committed, since `value` here is still the previous one.
          onSubmit={(iso) => onSubmit?.(snap.submit("from", iso))}
          onBlur={() => snap.blur("from")}
        />
        <DateInput
          key={`to-${snap.revision}`}
          aria-label={`${label}: ${t("valueTo")}`}
          value={value?.to}
          // Opens in the month of the range's start while the end is still empty.
          defaultMonth={value?.from}
          onChange={(iso) => snap.change("to", iso)}
          onSubmit={(iso) => onSubmit?.(snap.submit("to", iso))}
          onBlur={() => snap.blur("to")}
        />
      </RangeBounds>
      <PeriodStepper
        kinds={kinds}
        // An empty filter shows the list's default art as chosen, so the arrows and a typed date use it.
        current={
          periodOfDateValue(value, kinds, ctx) ??
          (defaultKind && !value?.from && !value?.to
            ? {
                kind: defaultKind,
                anchor: defaultKind.beginOf(todayOf(ctx), ctx),
              }
            : null)
        }
        // Follows the dates on screen: a start date typed by hand decides which month the arrows page
        // from, so one click cannot jump somewhere the user never named.
        anchor={anchorOfBounds(kinds[0], value?.from, value?.to, ctx)}
        // No `onSubmit`: paging must not close the pill it sits in (see [PeriodStepper]). The art travels
        // with the value, because one of them cannot be read back off the two dates (see
        // [periodOfDateValue]).
        onSelect={(kind, anchor) =>
          onChange({
            ...boundsOfPeriod(kind, anchor, ctx),
            periodKind: kind.id,
          })
        }
        // Releases the art for a free range, keeping the two dates: the marker stops the read-back that a
        // whole calendar month would otherwise give, so the begin can be retyped off the first of the month.
        onClear={() => onChange({ ...value, periodKind: CUSTOM_PERIOD_KIND })}
        // Room for the spelled-out art on its own line below the two dates, unlike the form grid.
        longLabel
      />
    </div>
  );
}
