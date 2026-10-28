"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import {
  ABSENT_BAR_CLASS,
  AVAILABILITY_BAR_CLASS,
  barClassName,
  dayClassName,
} from "./absence-chart-grid";

/**
 * What the colours of an absence chart mean (see VacationOverlapChart, AvailabilitySubstituteChart). The team calendar
 * (`team`, VacationTeamChart) has neither an entry being edited nor uncovered days, but may show availability entries
 * (`availabilities`), as the forms do for the substitutes' ones.
 *
 * @param ownLabel What the entry being edited is called, by default "This vacation".
 */
export function AbsenceChartLegend({
  team = false,
  availabilities = false,
  ownLabel,
}: {
  team?: boolean;
  availabilities?: boolean;
  ownLabel?: string;
}) {
  const t = useTranslations();
  const items = [
    ...(team
      ? []
      : [
          {
            className: barClassName(true, true),
            label: ownLabel ?? t("vacation.chart.own"),
          },
        ]),
    {
      className: barClassName(false, true),
      label: t("vacation.status.approved"),
    },
    {
      className: barClassName(false, false),
      label: t("vacation.status.inProgress"),
    },
    ...(team
      ? []
      : [
          {
            className: barClassName(true, true, true),
            label: t("vacation.chart.uncovered"),
          },
        ]),
    ...(availabilities
      ? [
          {
            className: ABSENT_BAR_CLASS,
            label: t("availability.status.notAvailable"),
          },
          {
            className: AVAILABILITY_BAR_CLASS,
            label: t("availability.title._"),
          },
        ]
      : []),
    {
      className: dayClassName({ workingDay: false }),
      label: t("vacation.chart.nonWorkingDay"),
    },
  ];
  return (
    <figcaption className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
      {items.map((item) => (
        <span key={item.label} className="inline-flex items-center gap-1.5">
          <span
            aria-hidden
            className={cn("inline-block h-3 w-4 rounded-sm", item.className)}
          />
          {item.label}
        </span>
      ))}
    </figcaption>
  );
}
