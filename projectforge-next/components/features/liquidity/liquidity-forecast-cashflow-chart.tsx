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
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  type ChartConfig,
} from "@/components/ui/chart";
import { ChartValueTooltip } from "@/components/shared/chart/chart-value-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, formatDate } from "@/lib/format";
import { niceDateTicks, niceScale } from "@/lib/chart-scale";
import { CHART_ROLE } from "@/lib/charts/roles";
import type { LiquidityForecastDay } from "@/lib/rs/liquidity";

/**
 * The per-day expected cash flow — the successor of the Wicket `LiquidityChartBuilder` bar chart. Each day's
 * expected credit (money coming in, negative) and expected debit (money going out, positive) as bars, with
 * the expected running balance overlaid as a line. As with the balance chart, no paranoia-case series.
 */
export function LiquidityForecastCashflowChart({
  data,
}: {
  data: LiquidityForecastDay[];
}) {
  const t = useTranslations("plugins.liquidityplanning");
  const ctx = useFormatContext();
  const config: ChartConfig = {
    creditExpected: { label: t("common.credit"), color: CHART_ROLE.positive },
    debitExpected: { label: t("common.debit"), color: CHART_ROLE.negative },
    expectedBalance: {
      label: t("forecast.expected"),
      color: CHART_ROLE.neutral,
    },
  };
  const scale = useMemo(
    () =>
      niceScale(
        data.flatMap((d) => [
          d.creditExpected,
          d.debitExpected,
          d.expectedBalance,
        ])
      ),
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
      aria-label={t("forecast.cashflow")}
    >
      {/* barGap/barCategoryGap tightened from the recharts defaults (4px / 10%) so the credit and debit
          bars take up most of each day's slot rather than leaving it mostly whitespace. */}
      <ComposedChart
        data={data}
        margin={{ left: 4, right: 12, top: 8 }}
        barGap={0}
        barCategoryGap="8%"
      >
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
        <ChartLegend content={<ChartLegendContent />} />
        <Bar
          dataKey="creditExpected"
          fill="var(--color-creditExpected)"
          isAnimationActive={false}
        />
        <Bar
          dataKey="debitExpected"
          fill="var(--color-debitExpected)"
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
      </ComposedChart>
    </ChartContainer>
  );
}
