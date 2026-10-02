"use client";

import { useMemo } from "react";
import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Line,
  XAxis,
  YAxis,
} from "recharts";
import { useTranslations } from "next-intl";
import { ChartContainer, ChartLegend } from "@/components/ui/chart";
import { ChartValueTooltip } from "@/components/shared/chart/chart-value-tooltip";
import { SeriesLegendContent } from "@/components/shared/chart/series-legend-content";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { niceScale } from "@/lib/chart-scale";
import { CHART_BAR_FILL_OPACITY } from "@/lib/charts/series";
import type { ForecastChartData } from "@/lib/rs/order";
import {
  buildForecastChartConfig,
  formatChartMonth,
  lineDashArray,
  monthlyBarKeys,
  monthlyLineKeys,
  monthlyRows,
  spansMultipleYears,
} from "./order-forecast-series";

/**
 * The monthly forecast ('Umsatzprognose Monatswerte' of the Excel export, in its colours): the remaining
 * forecast of each position status and the invoiced sums (IST) as stacked, outlined bars, with the plan (if
 * a planning date is set) and the invoiced sums of the two previous years as lines.
 */
export function OrderForecastMonthlyChart({
  data,
}: {
  data: ForecastChartData;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const config = useMemo(() => buildForecastChartConfig(t), [t]);
  const rows = useMemo(() => monthlyRows(data), [data]);
  const bars = useMemo(() => monthlyBarKeys(data), [data]);
  const lines = useMemo(() => monthlyLineKeys(data), [data]);
  const showYear = spansMultipleYears(data);
  // The stacked bars reach up to their monthly sum, so the axis is scaled on that sum next to the lines.
  const scale = useMemo(
    () =>
      niceScale(
        rows.flatMap((row) => [
          bars.reduce((sum, key) => sum + ((row[key] as number) ?? 0), 0),
          ...lines.map((key) => row[key] as number),
        ])
      ),
    [rows, bars, lines]
  );
  return (
    <ChartContainer
      config={config}
      className="h-[27rem] w-full"
      role="img"
      aria-label={t("fibu.auftrag.forecast.chart.monthly")}
    >
      <ComposedChart
        data={rows}
        margin={{ left: 4, right: 12, top: 8 }}
        // Half of recharts' default bar width: a 30% gap on each side of a month leaves 40% for the bar.
        barCategoryGap="30%"
      >
        <CartesianGrid
          vertical={false}
          stroke="var(--muted-foreground)"
          strokeOpacity={0.35}
        />
        <XAxis
          dataKey="month"
          tickLine={false}
          axisLine={false}
          tickMargin={8}
          tickFormatter={(value) =>
            formatChartMonth(String(value), ctx, showYear)
          }
        />
        <YAxis
          width={96}
          tickLine={false}
          axisLine={false}
          domain={scale.domain}
          ticks={scale.ticks}
          tickFormatter={(value) => formatCurrency(value, ctx, 0)}
        />
        <ChartValueTooltip
          config={config}
          formatValue={(value) => formatCurrency(value, ctx, 0)}
          formatLabel={(label) => formatChartMonth(label, ctx, showYear)}
        />
        <ChartLegend
          content={<SeriesLegendContent config={config} reversed />}
        />
        {bars.map((key) => (
          <Bar
            key={key}
            dataKey={key}
            stackId="forecast"
            stroke={`var(--color-${key})`}
            strokeWidth={1.5}
            fill={`var(--color-${key})`}
            fillOpacity={CHART_BAR_FILL_OPACITY}
            isAnimationActive={false}
          />
        ))}
        {lines.map((key) => (
          <Line
            key={key}
            dataKey={key}
            type="linear"
            stroke={`var(--color-${key})`}
            strokeWidth={2}
            strokeDasharray={lineDashArray(key)}
            dot={false}
            isAnimationActive={false}
          />
        ))}
      </ComposedChart>
    </ChartContainer>
  );
}
