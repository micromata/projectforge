"use client";

import { useTranslations } from "next-intl";
import { DateInput } from "@/components/shared/date-input";
import { PeriodStepper } from "@/components/shared/period-stepper";
import { RangeBounds } from "@/components/shared/range-bounds";
import { useFormatContext } from "@/hooks/use-format";
import { anchorOfBounds, boundsOfPeriod } from "@/lib/date-period-bounds";
import type { FilterInputProps } from "./filter-field-inputs";
import { periodOfDateValue } from "./filter-period";
import { useFilterPeriodKinds } from "./filter-period-kinds";

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

  function next(part: "from" | "to", raw: string | null) {
    // The art falls away with a bound typed by hand, exactly as a term does on a form: the two dates are
    // the user's again, and "bis heute" must not drag the other end along tomorrow.
    const merged = {
      ...value,
      periodKind: undefined,
      [part]: raw ?? undefined,
    };
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
        kinds={kinds}
        current={periodOfDateValue(value, kinds, ctx)}
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
        // Room for the spelled-out art on its own line below the two dates, unlike the form grid.
        longLabel
      />
    </div>
  );
}
