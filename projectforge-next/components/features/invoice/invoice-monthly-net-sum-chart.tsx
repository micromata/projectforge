"use client";

import { useMemo } from "react";
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts";
import { useTranslations } from "next-intl";
import { ChartContainer, ChartLegend } from "@/components/ui/chart";
import { ChartValueTooltip } from "@/components/shared/chart/chart-value-tooltip";
import { SeriesLegendContent } from "@/components/shared/chart/series-legend-content";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { niceScale } from "@/lib/chart-scale";
import { CHART_BAR_FILL_OPACITY } from "@/lib/charts/series";
import type { InvoiceNetSumChartData } from "@/lib/rs/invoice";
import {
  buildInvoiceChartConfig,
  formatChartMonth,
  monthlyRows,
  seriesKeys,
  spansMultipleYears,
  yearDashArray,
} from "./invoice-net-sum-series";

/**
 * The monthly net sums as grouped bars — one bar per year in each month, oldest at the front of the group
 * and this year at the back. The bar counterpart of the cumulative curves ({@link InvoiceCumulativeNetSumChart}); the
 * two share their colours and their year labels (see `invoice-net-sum-series.ts`).
 */
export function InvoiceMonthlyNetSumChart({
  data,
}: {
  data: InvoiceNetSumChartData;
}) {
  const t = useTranslations("fibu.rechnung.chart");
  const ctx = useFormatContext();
  const config = useMemo(() => buildInvoiceChartConfig(data), [data]);
  const rows = useMemo(() => monthlyRows(data), [data]);
  const keys = useMemo(() => seriesKeys(data), [data]);
  const showYear = spansMultipleYears(data);
  const scale = useMemo(
    () =>
      niceScale(rows.flatMap((row) => keys.map((key) => row[key] as number))),
    [rows, keys]
  );
  return (
    <ChartContainer
      config={config}
      className="h-[27rem] w-full"
      role="img"
      aria-label={t("monthly")}
    >
      <BarChart data={rows} margin={{ left: 4, right: 12, top: 8 }} barGap={0}>
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
          <Bar
            key={key}
            dataKey={key}
            stroke={`var(--color-${key})`}
            strokeWidth={1.5}
            strokeDasharray={yearDashArray(key)}
            legendType="line"
            fill={`var(--color-${key})`}
            fillOpacity={CHART_BAR_FILL_OPACITY}
            isAnimationActive={false}
          />
        ))}
      </BarChart>
    </ChartContainer>
  );
}
