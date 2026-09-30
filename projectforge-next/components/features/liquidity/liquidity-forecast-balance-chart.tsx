"use client";

import { useMemo } from "react";
import { CartesianGrid, Line, LineChart, XAxis, YAxis } from "recharts";
import { useTranslations } from "next-intl";
import {
  ChartContainer,
  ChartLegend,
  type ChartConfig,
} from "@/components/ui/chart";
import { ChartValueTooltip } from "@/components/shared/chart/chart-value-tooltip";
import { SeriesLegendContent } from "@/components/shared/chart/series-legend-content";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatDate } from "@/lib/format";
import { niceDateTicks, niceScale } from "@/lib/chart-scale";
import { CHART_ROLE } from "@/lib/charts/roles";
import type { LiquidityForecastDay } from "@/lib/rs/liquidity";

/**
 * The cumulative liquidity balance over time — the successor of the Wicket `LiquidityChartBuilder` XY plot.
 * Two lines: the running balance by actual due date and the one by expected date of payment. The
 * "paranoia case" third line of the original is deliberately omitted (user decision).
 */
export function LiquidityForecastBalanceChart({
  data,
}: {
  data: LiquidityForecastDay[];
}) {
  const t = useTranslations("plugins.liquidityplanning.forecast");
  const ctx = useFormatContext();
  const config: ChartConfig = {
    dueDateBalance: { label: t("dueDate"), color: CHART_ROLE.positive },
    expectedBalance: { label: t("expected"), color: CHART_ROLE.neutral },
  };
  const scale = useMemo(
    () => niceScale(data.flatMap((d) => [d.dueDateBalance, d.expectedBalance])),
    [data]
  );
  const dateTicks = useMemo(
    () => niceDateTicks(data.map((d) => d.date)),
    [data]
  );
  return (
    <ChartContainer
      config={config}
      className="h-[27rem] w-full"
      role="img"
      aria-label={t("balance")}
    >
      <LineChart data={data} margin={{ left: 4, right: 12, top: 8 }}>
        <CartesianGrid
          vertical={false}
          stroke="var(--muted-foreground)"
          strokeOpacity={0.35}
        />
        <XAxis
          dataKey="date"
          tickLine={false}
          axisLine={false}
          ticks={dateTicks}
          interval={0}
          tickMargin={8}
          tickFormatter={(value) => formatDate(value, ctx)}
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
          formatLabel={(label) => formatDate(label, ctx)}
        />
        <ChartLegend content={<SeriesLegendContent config={config} />} />
        <Line
          dataKey="dueDateBalance"
          type="linear"
          stroke="var(--color-dueDateBalance)"
          strokeWidth={2}
          dot={false}
          isAnimationActive={false}
        />
        <Line
          dataKey="expectedBalance"
          type="linear"
          stroke="var(--color-expectedBalance)"
          strokeWidth={2}
          dot={false}
          isAnimationActive={false}
        />
      </LineChart>
    </ChartContainer>
  );
}
