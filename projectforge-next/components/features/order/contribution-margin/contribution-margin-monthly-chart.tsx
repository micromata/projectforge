"use client";

import { useMemo } from "react";
import {
  Bar,
  CartesianGrid,
  Cell,
  ComposedChart,
  Line,
  ReferenceLine,
  XAxis,
  YAxis,
} from "recharts";
import { useTranslations } from "next-intl";
import {
  ChartContainer,
  ChartLegend,
  type ChartConfig,
} from "@/components/ui/chart";
import { ChartValueTooltip } from "@/components/shared/chart/chart-value-tooltip";
import { SeriesLegendContent } from "@/components/shared/chart/series-legend-content";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatPercentage } from "@/lib/format";
import { CHART_BAR_FILL_OPACITY } from "@/lib/charts/series";
import type { ContributionMarginData } from "@/lib/rs/order";
import { formatChartMonth } from "../forecast-chart/order-forecast-series";
import {
  amountScale,
  BAR_KEYS,
  contributionMarginChartConfig,
  contributionMarginChartRows,
  PERCENT_DASH,
  LEGEND_ORDER,
  LINE_KEYS,
  PERCENT_AXIS,
  PERCENTAGE_KEYS,
  percentScale,
} from "./contribution-margin-chart-data";
import { ContributionMarginTargetBands } from "./contribution-margin-target-bands";

/**
 * The monthly contribution margin: revenue and costs of each month as outlined bars side by side, the
 * contribution margin (DB1) and DB % cumulated from the first month on as lines, with those of the previous
 * year. The bars of the preliminary months (after the last
 * imported accounting records) are drawn fainter with a dashed outline. The DB % runs on a percent axis
 * on the right, over faint red/yellow bands below the red threshold and the target. DB % lines are dotted,
 * DB1 lines solid; the colour tells the year (see YEAR_COLOR).
 */
export function ContributionMarginMonthlyChart({
  data,
  className = "h-[27rem] w-full",
}: {
  data: ContributionMarginData;
  /** Size of the chart, e.g. a dashboard tile's height step. */
  className?: string;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin");
  const ctx = useFormatContext();
  const config = useMemo<ChartConfig>(
    () => contributionMarginChartConfig(t),
    [t]
  );
  const rows = useMemo(() => contributionMarginChartRows(data), [data]);
  const scale = useMemo(() => amountScale(rows), [rows]);
  const percent = useMemo(
    () => percentScale(rows, data.targetPercentage),
    [rows, data.targetPercentage]
  );
  // The year is shown on the axis only if the 12 months cross a year boundary, as in the forecast charts.
  const showYear =
    new Set(data.months.map((month) => month.slice(0, 4))).size > 1;
  const preliminaryMonths = useMemo(
    () => new Set(data.months.filter((_, i) => data.preliminary[i])),
    [data]
  );
  const formatMonth = (month: string) => {
    const label = formatChartMonth(month, ctx, showYear);
    return preliminaryMonths.has(month)
      ? `${label} (${t("preliminary")})`
      : label;
  };
  return (
    <ChartContainer
      config={config}
      className={className}
      role="img"
      aria-label={t("monthly")}
    >
      <ComposedChart
        data={rows}
        margin={{ left: 4, right: 4, top: 8 }}
        barCategoryGap="20%"
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
        <YAxis
          yAxisId={PERCENT_AXIS}
          orientation="right"
          width={56}
          tickLine={false}
          axisLine={false}
          domain={percent.domain}
          ticks={percent.ticks}
          tickFormatter={(value) => formatPercentage(value, ctx)}
        />
        <ContributionMarginTargetBands
          limits={data}
          scale={percent}
          targetLabel={t("target", { arg0: data.targetPercentage })}
        />
        <ReferenceLine y={0} stroke="var(--muted-foreground)" />
        <ChartValueTooltip
          config={config}
          formatValue={(value, key) =>
            (PERCENTAGE_KEYS as readonly string[]).includes(key)
              ? formatPercentage(value, ctx)
              : formatCurrency(value, ctx, 0)
          }
          formatLabel={(label) => formatMonth(String(label))}
        />
        <ChartLegend
          content={<SeriesLegendContent config={config} order={LEGEND_ORDER} />}
        />
        {BAR_KEYS.map((key) => (
          <Bar
            key={key}
            dataKey={key}
            stroke={`var(--color-${key})`}
            strokeWidth={1.5}
            fill={`var(--color-${key})`}
            fillOpacity={CHART_BAR_FILL_OPACITY}
            isAnimationActive={false}
          >
            {rows.map((row) => (
              <Cell
                key={row.month}
                fillOpacity={
                  preliminaryMonths.has(row.month)
                    ? CHART_BAR_FILL_OPACITY / 2
                    : CHART_BAR_FILL_OPACITY
                }
                strokeDasharray={
                  preliminaryMonths.has(row.month) ? "4 3" : undefined
                }
              />
            ))}
          </Bar>
        ))}
        {LINE_KEYS.map((key) => (
          <Line
            key={key}
            dataKey={key}
            type="linear"
            stroke={`var(--color-${key})`}
            strokeWidth={2.5}
            dot={key === "profit"}
            isAnimationActive={false}
          />
        ))}
        {PERCENTAGE_KEYS.map((key) => (
          <Line
            key={key}
            yAxisId={PERCENT_AXIS}
            dataKey={key}
            type="linear"
            stroke={`var(--color-${key})`}
            strokeWidth={2.5}
            strokeDasharray={PERCENT_DASH}
            dot={key === "percentage"}
            connectNulls={false}
            isAnimationActive={false}
          />
        ))}
      </ComposedChart>
    </ChartContainer>
  );
}
