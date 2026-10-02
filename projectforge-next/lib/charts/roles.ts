/**
 * The shared colour foundation for role charts — charts whose series carry a fixed meaning rather than
 * being N interchangeable things (that is the categorical case, see `series.ts`). One entry point so a
 * "positive" series is the same green everywhere, an "actual" series the same green, and so on. The
 * colours themselves live as tokens in `app/globals.css` (`--chart-*`); this maps the meanings onto them.
 */
export const CHART_ROLE = {
  /** Money coming in / above target — the brand green. */
  positive: "var(--chart-positive)",
  /** Money going out / below target — the brand pink. */
  negative: "var(--chart-negative)",
  /** A balance below zero — the area between a running balance and the zero line is filled red. */
  deficit: "var(--chart-deficit)",
  /** A meaning-neutral reference series (e.g. an expected running balance) — a muted black/white. */
  neutral: "var(--chart-neutral)",
  /** The planned/target ("Soll") series of the discipline charts — red. */
  target: "var(--chart-soll)",
  /** The actually-booked ("Ist") series of the discipline charts — green. */
  actual: "var(--chart-ist)",
} as const;

/**
 * The recharts fill opacity of a translucent area: the series keeps its role colour as a vivid stroke but
 * fills it only faintly, so overlapping areas (the discipline charts' target vs. actual) stay legible.
 */
export const CHART_AREA_FILL_OPACITY = 0.12;
