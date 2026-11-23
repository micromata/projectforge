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
  hasForecastData,
  formatChartMonth,
  monthlyBarKeys,
  lineDashArray,
  monthlyCompareBarKeys,
  type SeriesKey,
  monthlyRows,
  spansMultipleYears,
} from "./order-forecast-series";

/**
 * The monthly forecast ('Umsatzprognose Monatswerte' of the Excel export, in its colours): per month the
 * remaining forecast of each position status and the invoiced sums (IST) as one stacked, outlined bar
 * (invoiced plus remaining forecast), preceded by an outlined bar of the plan (if a planning date is set) and followed by those of the two
 * previous years.
 */
export function OrderForecastMonthlyChart({
  data,
  className = "h-[27rem] w-full",
}: {
  data: ForecastChartData;
  /** Size of the chart, e.g. a dashboard tile's height step. */
  className?: string;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const config = useMemo(
    () => buildForecastChartConfig(t, data.months[0], hasForecastData(data)),
    [t, data.months]
  );
  const rows = useMemo(() => monthlyRows(data), [data]);
  const bars = useMemo(() => monthlyBarKeys(data), [data]);
  const compareBars = useMemo(() => monthlyCompareBarKeys(data), [data]);
  const showYear = spansMultipleYears(data);
  // The stacked bars reach up to their monthly sum, so the axis is scaled on that sum next to the comparison bars.
  const scale = useMemo(
    () =>
      niceScale(
        rows.flatMap((row) => [
          bars.reduce((sum, key) => sum + ((row[key] as number) ?? 0), 0),
          ...compareBars.map((key) => row[key] as number),
        ])
      ),
    [rows, bars, compareBars]
  );
  // Outlined bar of a previous year or the plan, standing beside the stack (a stack of its own).
  const compareBar = (key: SeriesKey) => (
    <Bar
      key={key}
      dataKey={key}
      // An own stack per bar: recharts places unstacked bars after all stacks, whatever their order here.
      stackId={key}
      stroke={`var(--color-${key})`}
      strokeWidth={1.5}
      strokeDasharray={lineDashArray(key)}
      // The years show their line style (dotted or solid) in the legend; the plan keeps its colour square.
      legendType={key === "plan" ? "rect" : "line"}
      fill={`var(--color-${key})`}
      fillOpacity={CHART_BAR_FILL_OPACITY / 3}
      isAnimationActive={false}
    />
  );
  return (
    <ChartContainer
      config={config}
      className={className}
      role="img"
      aria-label={t("fibu.auftrag.forecast.chart.monthly")}
    >
      <ComposedChart
        data={rows}
        margin={{ left: 4, right: 12, top: 8 }}
        // A 20% gap on each side of a month leaves 60% for the group of the stack and its comparison bars.
        barCategoryGap="20%"
        barGap={2}
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
          labels={{ ist: t("fibu.auftrag.forecast.chart.ist") }}
          config={config}
          formatValue={(value) => formatCurrency(value, ctx, 0)}
          formatLabel={(label) => formatChartMonth(label, ctx, showYear)}
        />
        <ChartLegend
          content={
            <SeriesLegendContent
              config={config}
              // As the bars from left to right: the plan, the stack from its top down, then the years newest first.
              order={[
                "plan",
                ...[...bars].reverse(),
                "prevYear",
                "prevPrevYear",
                "prevPrevPrevYear",
              ]}
            />
          }
        />
        {compareBars.includes("plan") && compareBar("plan")}
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
        {/* Invisible: only puts the sum of the stack (invoiced plus all forecast parts) into the tooltip. */}
        <Line
          dataKey="stackSum"
          stroke="none"
          dot={false}
          activeDot={false}
          legendType="none"
          isAnimationActive={false}
        />
        {compareBars.filter((key) => key !== "plan").map(compareBar)}
      </ComposedChart>
    </ChartContainer>
  );
}
