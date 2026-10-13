"use client";

import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import type { FilterElement, MagicFilterEntryValue } from "@/lib/rs/types";
import { useFilterPeriodKinds } from "./filter-period-kinds";
import { pageablePeriodOf, steppedPeriodValue } from "./filter-period";
import { steppedNumberOf } from "./filter-value";

/**
 * The arrows of a filter pill (see [FilterPillShell] onStep), paging its value without opening the popover:
 * a period in effect by its kind (month by month, so the list and the statistics above it follow), a whole
 * number with a step width (a year) by that width. None for anything else.
 */
export function useFilterPillStep(
  element: FilterElement,
  value: MagicFilterEntryValue | undefined,
  apply: (next: MagicFilterEntryValue) => void
): {
  onStep?: (steps: number) => void;
  previousLabel: string;
  nextLabel: string;
} {
  const t = useTranslations();
  const ctx = useFormatContext();
  const kinds = useFilterPeriodKinds();
  const period = pageablePeriodOf(value, kinds, ctx);
  if (period) {
    return {
      onStep: (steps) => {
        const next = steppedPeriodValue(value, steps, kinds, ctx);
        if (next) apply(next);
      },
      previousLabel: t(period.kind.tooltipPreviousKey ?? "duration.previous"),
      nextLabel: t(period.kind.tooltipNextKey ?? "duration.next"),
    };
  }
  const number = steppedNumberOf(element, value);
  if (number != null) {
    return {
      onStep: (steps) =>
        apply({ value: String(number + steps * element.stepWidth!) }),
      previousLabel: t("calendar.previousYear"),
      nextLabel: t("calendar.nextYear"),
    };
  }
  return {
    previousLabel: t("duration.previous"),
    nextLabel: t("duration.next"),
  };
}
