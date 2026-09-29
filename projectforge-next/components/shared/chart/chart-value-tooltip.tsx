"use client";

import {
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";

/**
 * The shared hover tooltip for a value chart: one row per series, its config label on the left and the
 * formatted value on the right (`font-mono tabular-nums` so the numbers line up). Wraps the shadcn
 * `ChartTooltip`/`ChartTooltipContent` pair that every chart otherwise copies verbatim; the two formatters
 * are the only things that differ between charts (currency vs. plain number, month vs. date label).
 *
 * Drop it in place of a hand-written `<ChartTooltip>` inside any recharts chart.
 */
export function ChartValueTooltip({
  config,
  formatValue,
  formatLabel,
}: {
  config: ChartConfig;
  formatValue: (value: number) => string;
  formatLabel?: (label: string) => string;
}) {
  return (
    <ChartTooltip
      content={
        <ChartTooltipContent
          labelFormatter={(value) =>
            formatLabel ? formatLabel(String(value)) : String(value)
          }
          formatter={(value, name) => (
            <span className="flex w-full justify-between gap-2">
              <span className="text-muted-foreground">
                {config[String(name)]?.label ?? String(name)}
              </span>
              <span className="font-mono font-medium tabular-nums">
                {formatValue(value as number)}
              </span>
            </span>
          )}
        />
      }
    />
  );
}
