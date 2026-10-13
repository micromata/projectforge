"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts";
import { ChartValueTooltip } from "@/components/shared/chart/chart-value-tooltip";
import { ChartContainer, type ChartConfig } from "@/components/ui/chart";
import { useFormatContext } from "@/hooks/use-format";
import { CHART_ROLE } from "@/lib/charts/roles";
import { formatDate, formatNumber, formatYearMonth } from "@/lib/format";
import type { SchedulerPeriodEntry } from "@/lib/rs/admin-scheduler";

/**
 * The runs of a job per day (or month) as stacked bars: the successful, the failed and the skipped ones, oldest
 * first. Days without runs aren't there, so a gap on the axis means the job didn't run.
 */
export function SchedulerRunsChart({
  title,
  periods,
  months = false,
}: {
  title: string;
  periods: SchedulerPeriodEntry[];
  months?: boolean;
}) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const config: ChartConfig = {
    success: {
      label: t("system.scheduler.runStatus.success"),
      color: CHART_ROLE.positive,
    },
    error: {
      label: t("system.scheduler.runStatus.error"),
      color: CHART_ROLE.negative,
    },
    skipped: {
      label: t("system.scheduler.runStatus.skipped"),
      color: CHART_ROLE.neutral,
    },
  };
  const data = useMemo(
    () =>
      periods.map((period) => ({
        period: period.periodStart,
        success: period.successCount,
        error: period.errorCount,
        skipped: period.skippedCount,
      })),
    [periods]
  );
  const formatPeriod = (value: unknown) =>
    months
      ? formatYearMonth(value, ctx)
      : // An ISO date without time: read in UTC, so no time zone shifts the day.
        formatDate(value, { ...ctx, timeZone: "UTC" });
  return (
    <div className="space-y-1">
      <div className="text-xs font-medium text-muted-foreground">{title}</div>
      {data.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          {t("system.scheduler.detail.noRuns")}
        </p>
      ) : (
        <ChartContainer
          config={config}
          className="h-40 w-full"
          role="img"
          aria-label={title}
        >
          <BarChart data={data} margin={{ left: 0, right: 8, top: 4 }}>
            <CartesianGrid vertical={false} />
            <XAxis
              dataKey="period"
              tickLine={false}
              axisLine={false}
              minTickGap={32}
              tickMargin={6}
              tickFormatter={formatPeriod}
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
              formatLabel={formatPeriod}
            />
            {(["success", "error", "skipped"] as const).map((key) => (
              <Bar
                key={key}
                dataKey={key}
                stackId="runs"
                fill={`var(--color-${key})`}
                isAnimationActive={false}
              />
            ))}
          </BarChart>
        </ChartContainer>
      )}
    </div>
  );
}
