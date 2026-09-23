"use client";

import { Area, AreaChart, CartesianGrid, XAxis, YAxis } from "recharts";
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate, formatNumber } from "@/lib/format";

/** One of the two plotted series ("Soll" / "Ist"); `color` is a CSS variable, e.g. `var(--chart-soll)`. */
export interface DisciplineSeries {
  key: string;
  label: string;
  color: string;
}

interface DisciplineChartProps {
  /** Rows with a `date` (ISO `yyyy-MM-dd`) and one numeric field per series key. */
  data: Array<Record<string, string | number>>;
  /** Exactly the two series to draw, in painting order. */
  series: [DisciplineSeries, DisciplineSeries];
  /** Y-axis unit shown as the axis label, e.g. "hours" / "days". */
  unitLabel: string;
  /** Fraction digits of the value labels (1 for the latency in days, 0 for whole hours). */
  fractionDigits: number;
  /** Accessible name of the chart (the chart title). */
  ariaLabel: string;
}

/**
 * A "timesheet discipline" chart: the planned/target series and the actual series over time as two
 * translucent areas, so the gap between them — the point of the chart — reads at a glance. This is the
 * recharts successor of the former JFreeChart `XYDifferenceRenderer`; the two-tone difference band of the
 * original is approximated by the overlapping fills rather than reproduced pixel for pixel.
 */
export function DisciplineChart({
  data,
  series,
  unitLabel,
  fractionDigits,
  ariaLabel,
}: DisciplineChartProps) {
  const ctx = useFormatContext();
  const config: ChartConfig = {
    [series[0].key]: { label: series[0].label, color: series[0].color },
    [series[1].key]: { label: series[1].label, color: series[1].color },
  };
  return (
    <ChartContainer
      config={config}
      className="h-56 w-full"
      role="img"
      aria-label={ariaLabel}
    >
      <AreaChart data={data} margin={{ left: 4, right: 12, top: 8 }}>
        <CartesianGrid vertical={false} />
        <XAxis
          dataKey="date"
          tickLine={false}
          axisLine={false}
          minTickGap={48}
          tickMargin={8}
          tickFormatter={(value) => formatDate(value, ctx)}
        />
        <YAxis
          width={48}
          tickLine={false}
          axisLine={false}
          label={{ value: unitLabel, angle: -90, position: "insideLeft" }}
          tickFormatter={(value) => formatNumber(value, ctx, fractionDigits)}
        />
        <ChartTooltip
          content={
            <ChartTooltipContent
              labelFormatter={(value) => formatDate(value, ctx)}
              formatter={(value, name) => (
                <span className="flex w-full justify-between gap-2">
                  <span className="text-muted-foreground">
                    {config[String(name)]?.label ?? String(name)}
                  </span>
                  <span className="font-mono font-medium tabular-nums">
                    {formatNumber(value as number, ctx, fractionDigits)}
                  </span>
                </span>
              )}
            />
          }
        />
        <ChartLegend content={<ChartLegendContent />} />
        {series.map((s) => (
          <Area
            key={s.key}
            dataKey={s.key}
            type="linear"
            stroke={`var(--color-${s.key})`}
            fill={`var(--color-${s.key})`}
            fillOpacity={0.12}
            strokeWidth={2}
            dot={false}
            isAnimationActive={false}
          />
        ))}
      </AreaChart>
    </ChartContainer>
  );
}
