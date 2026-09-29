"use client";

import type { ComponentProps } from "react";
import { ChartLegendContent } from "@/components/ui/chart";

/**
 * A legend that reverses Recharts' default entry order. Recharts builds the legend payload
 * newest-year-first even when the bars/lines are drawn oldest-first, so the legend and the marks
 * disagree. Reversing the payload makes the legend read oldest-first too, matching the order the marks
 * are drawn in. Everything else is delegated to the shared {@link ChartLegendContent}; the `payload` prop
 * is injected by Recharts when this is used as a `<ChartLegend content={...} />`.
 */
export function ReversedChartLegendContent(
  props: ComponentProps<typeof ChartLegendContent>
) {
  const payload = props.payload ? [...props.payload].reverse() : props.payload;
  return <ChartLegendContent {...props} payload={payload} />;
}
