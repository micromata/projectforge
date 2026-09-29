"use client";

import { useMemo } from "react";
import { CartesianGrid, Line, LineChart, XAxis, YAxis } from "recharts";
import { useTranslations } from "next-intl";
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
} from "@/components/ui/chart";
import { ChartValueTooltip } from "@/components/shared/chart/chart-value-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency } from "@/lib/format";
import { niceScale } from "@/lib/chart-scale";
import { buildChartConfig } from "@/lib/charts/series";
import type { InvoiceNetSumChartData } from "@/lib/rs/invoice";
import {
  cumulativeRows,
  formatChartMonth,
  seriesKeys,
  spansMultipleYears,
} from "./invoice-net-sum-series";

/**
 * The year-to-date net sums as curves — each year's running total across the months, so the four years'
 * trajectories can be compared at a glance. The cumulative counterpart of the monthly bars
 * ({@link InvoiceMonthlyNetSumChart}); the two share their colours and their year labels.
 */
export function InvoiceCumulativeNetSumChart({
  data,
}: {
  data: InvoiceNetSumChartData;
}) {
  const t = useTranslations("fibu.rechnung.chart");
  const ctx = useFormatContext();
  const config = useMemo(() => buildChartConfig(data.series), [data]);
  const rows = useMemo(() => cumulativeRows(data), [data]);
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
      aria-label={t("cumulative")}
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
        <ChartLegend content={<ChartLegendContent />} />
        {keys.map((key) => (
          <Line
            key={key}
            dataKey={key}
            type="linear"
            stroke={`var(--color-${key})`}
            strokeWidth={2}
            dot={false}
            isAnimationActive={false}
          />
        ))}
      </LineChart>
    </ChartContainer>
  );
}
