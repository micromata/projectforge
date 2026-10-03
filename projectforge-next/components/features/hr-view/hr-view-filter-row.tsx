"use client";

import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { DateInput } from "@/components/shared/date-input";
import { PeriodStepper } from "@/components/shared/period-stepper";
import { RangeBounds } from "@/components/shared/range-bounds";
import { useFormatContext } from "@/hooks/use-format";
import { periodKindsOf } from "@/lib/date-period";
import {
  anchorOfBounds,
  boundsOfPeriod,
  periodOfBounds,
  shiftBounds,
} from "@/lib/date-period-bounds";
import type { HrViewFilter } from "./types";

// The week first: it is what the arrows page in while the period is none of these (Wicket's
// QuickSelectWeekPanel). The month is offered for a longer look ahead.
const KINDS = periodKindsOf(["week", "month"]);

type Option = Exclude<keyof HrViewFilter, "startDay" | "stopDay">;

const OPTIONS: { key: Option; label: string }[] = [
  { key: "showPlanning", label: "hr.planning.filter.showPlanning" },
  {
    key: "showBookedTimesheets",
    label: "hr.planning.filter.showBookedTimesheets",
  },
  { key: "onlyMyProjects", label: "hr.planning.filter.onlyMyProjects" },
  {
    key: "allProjectsGroupedByCustomer",
    label: "hr.planning.filter.allProjectsGroupedByCustomer",
  },
  {
    key: "otherProjectsGroupedByCustomer",
    label: "hr.planning.filter.otherProjectsGroupedByCustomer",
  },
];

/**
 * The options of the HR view: the period (two dates, paged by week or month, its calendar weeks next to
 * it) and the switches of the legacy page. Shows the effective filter the view answered with; each change
 * passes the whole filter on, so nothing falls back to the stored one.
 */
export function HrViewFilterRow({
  filter,
  calendarWeeks,
  onChange,
}: {
  filter: HrViewFilter;
  calendarWeeks: string;
  onChange: (filter: HrViewFilter) => void;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const { startDay, stopDay } = filter;

  function setPeriod(bounds: { from: string; to: string } | null) {
    if (bounds)
      onChange({ ...filter, startDay: bounds.from, stopDay: bounds.to });
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-end gap-4">
        <div className="flex flex-col gap-1">
          <Label>{t("timePeriod")}</Label>
          {/* A width of its own: the bounds are a size container, which in a flex row has none and would
              stack and clip the two boxes. Two date boxes (max-w-32) and the dash fit side by side. */}
          <RangeBounds breakpoint="@2xs" className="w-72 max-w-full">
            <DateInput
              value={startDay}
              required
              aria-label={t("timePeriod")}
              onChange={(value) =>
                value && onChange({ ...filter, startDay: value })
              }
            />
            <DateInput
              value={stopDay}
              defaultMonth={startDay}
              required
              aria-label={t("timePeriod")}
              onChange={(value) =>
                value && onChange({ ...filter, stopDay: value })
              }
            />
          </RangeBounds>
        </div>
        <PeriodStepper
          className="pb-0.5"
          kinds={KINDS}
          current={periodOfBounds(startDay, stopDay, KINDS, ctx)}
          anchor={anchorOfBounds(KINDS[0], startDay, stopDay, ctx)}
          onSelect={(kind, anchor) =>
            setPeriod(boundsOfPeriod(kind, anchor, ctx))
          }
          onStep={(steps) => {
            const current = periodOfBounds(startDay, stopDay, KINDS, ctx);
            setPeriod(
              shiftBounds(startDay, stopDay, current?.kind ?? null, steps, ctx)
            );
          }}
          canStep
          longLabel
        />
        <span className="pb-2 text-sm text-muted-foreground">
          {calendarWeeks}
        </span>
      </div>
      <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
        {OPTIONS.map(({ key, label }) => (
          <div key={key} className="flex items-center gap-2">
            <Switch
              id={`hr-view-${key}`}
              checked={filter[key]}
              onCheckedChange={(checked) =>
                onChange({ ...filter, [key]: checked })
              }
            />
            <Label htmlFor={`hr-view-${key}`}>{t(label)}</Label>
          </div>
        ))}
      </div>
    </div>
  );
}
