"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts";
import { ChartValueTooltip } from "@/components/shared/chart/chart-value-tooltip";
import { ChartContainer, type ChartConfig } from "@/components/ui/chart";
import { useFormatContext } from "@/hooks/use-format";
import { CHART_ROLE } from "@/lib/charts/roles";
import { formatDate, formatNumber, formatTimestampMinutes } from "@/lib/format";

const HOUR_MS = 60 * 60 * 1000;

/** The occurrences of a problem per bin of [binHours] hours as bars; the bins start at [start], oldest first. */
export function OccurrenceChart({
  title,
  values,
  start,
  binHours,
}: {
  title: string;
  values: number[];
  start: number;
  binHours: number;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const config: ChartConfig = {
    count: {
      label: t("system.admin.adminErrors.occurrences"),
      color: CHART_ROLE.negative,
    },
  };
  const data = useMemo(
    () =>
      values.map((count, index) => ({
        time: start + index * binHours * HOUR_MS,
        count,
      })),
    [values, start, binHours]
  );
  return (
    <div className="space-y-1">
      <div className="text-xs font-medium text-muted-foreground">{title}</div>
      <ChartContainer
        config={config}
        className="h-40 w-full"
        role="img"
        aria-label={title}
      >
        <BarChart data={data} margin={{ left: 0, right: 8, top: 4 }}>
          <CartesianGrid vertical={false} />
          <XAxis
            dataKey="time"
            tickLine={false}
            axisLine={false}
            minTickGap={32}
            tickMargin={6}
            tickFormatter={(value) => formatDate(value, ctx)}
          />
          <YAxis
            width={64}
            tickLine={false}
            axisLine={false}
            allowDecimals={false}
            tickFormatter={(value) => formatNumber(value, ctx, 0)}
          />
          <ChartValueTooltip
            config={config}
            formatValue={(value) => formatNumber(value, ctx, 0)}
            formatLabel={(label) =>
              binHours < 24
                ? formatTimestampMinutes(Number(label), ctx)
                : formatDate(Number(label), ctx)
            }
          />
          <Bar
            dataKey="count"
            fill="var(--color-count)"
            isAnimationActive={false}
          />
        </BarChart>
      </ChartContainer>
    </div>
  );
}
