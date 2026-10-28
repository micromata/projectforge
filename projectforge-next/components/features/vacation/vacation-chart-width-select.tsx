"use client";

import { useTranslations } from "next-intl";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { Label } from "@/components/ui/label";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import {
  chartWidthOf,
  type ChartWidth,
} from "@/components/shared/absence-chart/absence-chart-width";

/** How wide the team calendar draws its day columns, from fitting the window to wide (see ChartWidth). */
export function VacationChartWidthSelect({
  value,
  onChange,
}: {
  value: ChartWidth;
  onChange: (value: ChartWidth) => void;
}) {
  const t = useTranslations();
  const id = "vacation-export-day-width";
  // Spelled out, so the i18n scanner finds every key.
  const labels: Record<ChartWidth, string> = {
    auto: t("vacation.export.dayWidth.auto"),
    fit: t("vacation.export.dayWidth.fit"),
    narrow: t("vacation.export.dayWidth.narrow"),
    normal: t("vacation.export.dayWidth.normal"),
    wide: t("vacation.export.dayWidth.wide"),
  };
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={id}>{t("vacation.export.dayWidth._")}</Label>
      <Select value={value} onValueChange={(it) => onChange(chartWidthOf(it))}>
        <SelectTrigger id={id} className="w-auto">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {(Object.keys(labels) as ChartWidth[]).map((width) => (
            <SelectItem key={width} value={width}>
              {labels[width]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
