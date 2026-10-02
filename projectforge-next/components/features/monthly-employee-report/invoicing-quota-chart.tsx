"use client";

import { useMemo } from "react";
import { CartesianGrid, Line, LineChart, XAxis, YAxis } from "recharts";
import { useTranslations } from "next-intl";
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
} from "@/components/ui/chart";
import { useFormatContext } from "@/hooks/use-format";
import { formatPercentageDecimal, type FormatContext } from "@/lib/format";
import { buildChartConfig, seriesKey } from "@/lib/charts/series";
import type { InvoicingQuotaHistory } from "./types";

const QUOTA_KEY = seriesKey(0);
const TICKS = [0, 0.25, 0.5, 0.75, 1];

/** A chart month label, e.g. "Jan. '26" (`isoMonth` is `yyyy-MM`). */
function formatMonth(isoMonth: string, ctx: FormatContext): string {
  const [year, month] = isoMonth.split("-");
  const name = new Intl.DateTimeFormat(ctx.locale, { month: "short" }).format(
    new Date(Number(year), Number(month) - 1, 15)
  );
  return `${name} '${year.slice(2)}`;
}

/**
 * The invoicing quota of the last 12 months as one line of straight segments (no smoothing: each point is a
 * month of its own, not a sample of a continuous curve). A month without work time has no quota and leaves a
 * gap. The y axis is fixed to 0–100 %, so months and users compare at the same scale.
 */
export function InvoicingQuotaChart({ data }: { data: InvoicingQuotaHistory }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const config = useMemo(
    () => buildChartConfig([{ label: t("fibu.common.invoicingQuota._") }]),
    [t]
  );
  const rows = useMemo(
    () =>
      data.months.map((month) => ({
        month: month.month,
        [QUOTA_KEY]: month.quota,
        billedHours: month.billedHours,
        totalHours: month.totalHours,
      })),
    [data]
  );
  return (
    <ChartContainer
      config={config}
      className="h-[24rem] w-full"
      role="img"
      aria-label={t("fibu.monthlyEmployeeReport.invoicingQuotaChart.title")}
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
          tickFormatter={(value) => formatMonth(String(value), ctx)}
        />
        <YAxis
          width={56}
          tickLine={false}
          axisLine={false}
          domain={[0, 1]}
          ticks={TICKS}
          tickFormatter={(value) => formatPercentageDecimal(value, ctx, 0)}
        />
        <ChartTooltip
          content={
            <ChartTooltipContent
              labelFormatter={(value) => formatMonth(String(value), ctx)}
              formatter={(value, _name, item) => (
                <span className="flex w-full flex-col gap-0.5">
                  <span className="flex justify-between gap-2">
                    <span className="text-muted-foreground">
                      {config[QUOTA_KEY]?.label}
                    </span>
                    <span className="font-mono font-medium tabular-nums">
                      {formatPercentageDecimal(value, ctx)}
                    </span>
                  </span>
                  <span className="text-muted-foreground">
                    {t(
                      "fibu.monthlyEmployeeReport.invoicingQuotaChart.billed",
                      {
                        arg0: item.payload.billedHours,
                        arg1: item.payload.totalHours,
                      }
                    )}
                  </span>
                </span>
              )}
            />
          }
        />
        <Line
          dataKey={QUOTA_KEY}
          type="linear"
          stroke={`var(--color-${QUOTA_KEY})`}
          strokeWidth={2}
          dot={{ r: 3, fill: `var(--color-${QUOTA_KEY})` }}
          connectNulls={false}
          isAnimationActive={false}
        />
      </LineChart>
    </ChartContainer>
  );
}
