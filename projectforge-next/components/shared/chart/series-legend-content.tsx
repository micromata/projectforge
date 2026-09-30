"use client";

import type { DefaultLegendContentProps, LegendPayload } from "recharts";
import type { ChartConfig } from "@/components/ui/chart";
import { cn } from "@/lib/utils";

/**
 * The shared chart legend: one entry per series with its config label. A line series (recharts legend type
 * `line`, the default of `Line`) shows a short stroke in its colour and dash pattern, so a dashed or dotted
 * curve can be told apart from a solid one; every other series (bars, areas with `legendType="rect"`) shows
 * the colour square of the shadcn `ChartLegendContent`.
 *
 * Use it as `<ChartLegend content={<SeriesLegendContent config={config} />} />`; recharts injects `payload`.
 * Pass `reversed` if the marks are drawn oldest-first: recharts builds the payload in the opposite order, so
 * the legend would disagree with the drawing order otherwise.
 */
export function SeriesLegendContent({
  config,
  reversed = false,
  payload,
  verticalAlign = "bottom",
}: {
  config: ChartConfig;
  reversed?: boolean;
} & Pick<DefaultLegendContentProps, "payload" | "verticalAlign">) {
  if (!payload?.length) {
    return null;
  }
  const items = payload.filter((item) => item.type !== "none");
  if (reversed) {
    items.reverse();
  }
  return (
    <div
      className={cn(
        "flex flex-wrap items-center justify-center gap-4",
        verticalAlign === "top" ? "pb-3" : "pt-3"
      )}
    >
      {items.map((item, index) => {
        const key = String(item.dataKey ?? item.value ?? index);
        return (
          <div key={key} className="flex items-center gap-1.5">
            {item.type === "line" ? (
              <LineSwatch item={item} />
            ) : (
              <div
                className="h-2 w-2 shrink-0 rounded-[2px]"
                style={{ backgroundColor: item.color }}
              />
            )}
            {config[key]?.label ?? item.value}
          </div>
        );
      })}
    </div>
  );
}

/** A short stroke in the colour and dash pattern of the line series (taken from its props in the payload). */
function LineSwatch({ item }: { item: LegendPayload }) {
  const dashArray = (item.payload as { strokeDasharray?: string | number })
    ?.strokeDasharray;
  return (
    <svg
      width="20"
      height="4"
      viewBox="0 0 20 4"
      className="shrink-0"
      aria-hidden
    >
      <line
        x1="0"
        y1="2"
        x2="20"
        y2="2"
        stroke={item.color}
        strokeWidth={dashArray ? 2 : 3}
        strokeDasharray={dashArray}
      />
    </svg>
  );
}
