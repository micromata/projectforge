import type { ChartConfig } from "@/components/ui/chart";

/**
 * The shared colour and config foundation for categorical charts — any chart that compares a handful of
 * like series (years side by side, categories, …). One palette and one config builder so every such chart
 * across the app draws the same series in the same colours. The colours themselves live as tokens in
 * `app/globals.css` (`--chart-series-*`); this is their single TypeScript entry point.
 */

/** The categorical series colours, in order. Series beyond the fourth wrap around (see {@link buildChartConfig}). */
export const SERIES_PALETTE = [
  "var(--chart-series-1)",
  "var(--chart-series-2)",
  "var(--chart-series-3)",
  "var(--chart-series-4)",
];

/**
 * The recharts fill opacity of an outlined bar: the bar keeps its series colour as a vivid stroke but fills
 * it only faintly, so a whole axis of bars reads as outlines rather than solid blocks of colour.
 */
export const CHART_BAR_FILL_OPACITY = 0.18;

/** The recharts data key of series `i` (`s0` is the first series, `s1` the second, and so on). */
export function seriesKey(index: number): string {
  return `s${index}`;
}

/**
 * A {@link ChartConfig} for a list of series: each labelled by its own `label` and coloured by its rank in
 * the {@link SERIES_PALETTE} (wrapping past the fourth). `ChartContainer` turns each entry into a
 * `--color-<key>` CSS variable, so marks reference `var(--color-s0)` etc.
 */
export function buildChartConfig(series: { label: string }[]): ChartConfig {
  const config: ChartConfig = {};
  series.forEach((serie, index) => {
    config[seriesKey(index)] = {
      label: serie.label,
      color: SERIES_PALETTE[index % SERIES_PALETTE.length],
    };
  });
  return config;
}
