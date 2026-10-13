"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { barClassName, dayClassName } from "./vacation-chart-grid";

/** What the colours of the vacation chart mean (see VacationOverlapChart). */
export function VacationChartLegend() {
  const t = useTranslations();
  const items = [
    { className: barClassName(true, true), label: t("vacation.chart.own") },
    {
      className: barClassName(false, true),
      label: t("vacation.status.approved"),
    },
    {
      className: barClassName(false, false),
      label: t("vacation.status.inProgress"),
    },
    {
      className: barClassName(true, true, true),
      label: t("vacation.chart.uncovered"),
    },
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
