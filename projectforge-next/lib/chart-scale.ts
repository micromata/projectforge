/**
 * A "nice" linear axis scale for a numeric chart axis, shared by any recharts chart that wants round tick
 * labels. Recharts' own auto-ticks divide the raw data range into equal parts, which yields crooked labels
 * (1.050.000 €, 2.100.000 €); an axis read at a glance wants round steps. This rounds the step to the
 * nearest 1/2/5 × 10ⁿ and the bounds to a multiple of it, so the ticks land on 100k / 200k / 500k / 1M …
 * and always include 0.
 */
export interface AxisScale {
  domain: [number, number];
  ticks: number[];
}

/** The step rounded up to the next 1, 2 or 5 times a power of ten (so 130k → 200k, 260k → 500k). */
function niceStep(raw: number): number {
  const magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
  const normalized = raw / magnitude;
  const factor =
    normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
  return factor * magnitude;
}

/**
 * @param values every plotted value; 0 is added so a chart whose data never crosses it still shows the
 *   zero line (e.g. bars that grow from 0).
 * @param targetTicks the tick count to aim for — the actual count varies once the step is rounded.
 */
export function niceScale(values: number[], targetTicks = 6): AxisScale {
  const finite = values.filter((v) => Number.isFinite(v)).concat(0);
  const min = Math.min(...finite);
  const max = Math.max(...finite);
  // No range to divide (every value is 0, since 0 is always in the set); give it a symmetric window so
  // the flat line sits in the middle rather than dividing by a zero step.
  if (min === max) {
    const step = niceStep(Math.abs(min) || 1);
    return {
      domain: [min - step, max + step],
      ticks: [min - step, min, min + step],
    };
  }
  const step = niceStep((max - min) / targetTicks);
  const niceMin = Math.floor(min / step) * step;
  const niceMax = Math.ceil(max / step) * step;
  const ticks: number[] = [];
  // Counted, not accumulated, to keep the ticks exact multiples of the step (no floating-point drift).
  for (let i = 0; niceMin + i * step <= niceMax + step / 2; i++) {
    ticks.push(niceMin + i * step);
  }
  return { domain: [niceMin, niceMax], ticks };
}

/** Parse a `yyyy-MM-dd` string to a UTC-midnight Date; format one back. UTC avoids time-zone drift. */
function parseISODate(iso: string): Date {
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(Date.UTC(y, m - 1, d));
}
function toISODate(date: Date): string {
  return date.toISOString().slice(0, 10);
}
function addDays(date: Date, days: number): Date {
  return new Date(date.getTime() + days * 86_400_000);
}

/**
 * A calendar step for the date-axis ticks: either a fixed number of days (weeks are aligned to Monday) or a
 * number of months (aligned to the 1st, Jan-anchored so quarters land on Jan/Apr/Jul/Oct etc.).
 */
type DateStep = { days: number; monday?: boolean } | { months: number };

/** The step ladder, coarsest span first per unit — the one landing closest to `targetTicks` wins. */
const DATE_STEPS: DateStep[] = [
  { days: 1 },
  { days: 2 },
  { days: 7, monday: true },
  { days: 14, monday: true },
  { months: 1 },
  { months: 2 },
  { months: 3 },
  { months: 6 },
  { months: 12 },
];

/** The tick dates a single step produces across `[min, max]`, on calendar boundaries. */
function stepTicks(step: DateStep, min: Date, max: Date): string[] {
  const ticks: string[] = [];
  if ("months" in step) {
    let index = min.getUTCFullYear() * 12 + min.getUTCMonth();
    index -= ((index % step.months) + step.months) % step.months; // align down, Jan-anchored
    for (;;) {
      const date = new Date(Date.UTC(Math.floor(index / 12), index % 12, 1));
      if (date > max) break;
      if (date >= min) ticks.push(toISODate(date));
      index += step.months;
    }
    return ticks;
  }
  let start = min;
  if (step.monday) {
    start = addDays(min, (8 - min.getUTCDay()) % 7); // 0 = Sunday … so Monday is day 1
  }
  for (let date = start; date <= max; date = addDays(date, step.days)) {
    ticks.push(toISODate(date));
  }
  return ticks;
}

/**
 * "Nice" tick dates for a date axis, the date counterpart of {@link niceScale}: recharts spaces category
 * ticks by count, so with one point per day they fall on arbitrary days (11.07., 07.08., …). This snaps
 * them to calendar boundaries — month firsts, or Monday-aligned weeks over a short span — by choosing the
 * step whose tick count is closest to `targetTicks`. Only dates present in `isoDates` are returned, so the
 * result is safe to hand to a category axis's `ticks`.
 *
 * @param isoDates every plotted date as `yyyy-MM-dd` (need not be sorted or unique).
 */
export function niceDateTicks(isoDates: string[], targetTicks = 7): string[] {
  const present = new Set(isoDates);
  const sorted = [...present].sort();
  if (sorted.length <= targetTicks) {
    return sorted;
  }
  const min = parseISODate(sorted[0]);
  const max = parseISODate(sorted[sorted.length - 1]);
  let best: string[] = sorted;
  let bestDiff = Infinity;
  for (const step of DATE_STEPS) {
    const ticks = stepTicks(step, min, max).filter((iso) => present.has(iso));
    if (ticks.length < 2) continue;
    const diff = Math.abs(ticks.length - targetTicks);
    if (diff < bestDiff) {
      bestDiff = diff;
      best = ticks;
    }
  }
  return best;
}
