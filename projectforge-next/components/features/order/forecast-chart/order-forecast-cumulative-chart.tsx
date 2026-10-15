"use client";

import { useMemo } from "react";
import { CartesianGrid, Line, LineChart, XAxis, YAxis } from "recharts";
import { useTranslations } from "next-intl";
import { ChartContainer, ChartLegend } from "@/components/ui/chart";
import { ChartValueTooltip } from "@/components/shared/chart/chart-value-tooltip";
import { SeriesLegendContent } from "@/components/shared/chart/series-legend-content";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { niceScale } from "@/lib/chart-scale";
import type { ForecastChartData } from "@/lib/rs/order";
import {
  buildForecastChartConfig,
  cumulativeLineKeys,
  cumulativeRows,
  formatChartMonth,
  lineDashArray,
  spansMultipleYears,
} from "./order-forecast-series";

/**
 * The cumulated forecast ('Umsatzprognose kumuliert' of the Excel export): the running totals of the
 * forecast (per month the max of invoiced and remaining forecast), the plan (if a planning date is set) and
 * the invoiced sums of the two previous years. The counterpart of {@link OrderForecastMonthlyChart}.
 */
export function OrderForecastCumulativeChart({
  data,
  className = "h-[27rem] w-full",
}: {
  data: ForecastChartData;
  /** Size of the chart, e.g. a dashboard tile's height step. */
  className?: string;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const config = useMemo(() => buildForecastChartConfig(t), [t]);
  const rows = useMemo(() => cumulativeRows(data), [data]);
  const keys = useMemo(() => cumulativeLineKeys(data), [data]);
  const showYear = spansMultipleYears(data);
  const scale = useMemo(
    () =>
      niceScale(rows.flatMap((row) => keys.map((key) => row[key] as number))),
    [rows, keys]
  );
  return (
    <ChartContainer
      config={config}
      className={className}
      role="img"
      aria-label={t("fibu.auftrag.forecast.chart.cumulative")}
    >
      <LineChart data={rows} margin={{ left: 4, right: 12, top: 8 }}>
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
        {keys.map((key) => (
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
      </LineChart>
    </ChartContainer>
  );
}
