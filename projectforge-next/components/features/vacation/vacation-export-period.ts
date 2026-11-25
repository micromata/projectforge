import {
  CUSTOM_PERIOD_KIND,
  periodKindOf,
  type PeriodKind,
} from "@/lib/date-period";
import {
  boundsOfPeriod,
  currentAnchorOf,
  shiftBounds,
} from "@/lib/date-period-bounds";
import { isIsoDate, plusDays } from "@/lib/date-period-math";
import type { FormatContext } from "@/lib/format";

/**
 * The most the backend answers for at once: the end is `from + MAX_SPAN_DAYS` at the latest
 * (`VacationExportRest.MAX_DAYS`). Longer, it would silently cut the end off, so the page does it visibly.
 */
export const MAX_SPAN_DAYS = 400;

/**
 * What the team vacation calendar shows: a whole period of a `kind` (quarter, calendar year), or — with no
 * kind — any range the user entered, e.g. over the turn of the year. `from`/`until` are always the two days.
 */
export interface ExportPeriod {
  kind: PeriodKind | null;
  from: string;
  until: string;
}

/** A whole period of `kind` beginning at (or containing) `anchor`. */
export function periodOfKind(
  kind: PeriodKind,
  anchor: string,
  ctx: FormatContext
): ExportPeriod {
  const bounds = boundsOfPeriod(kind, anchor, ctx);
  return { kind, from: bounds.from, until: bounds.to };
}

/**
 * A free range, kept inside what may be shown: not before `min`, not ending before it begins and no longer
 * than [MAX_SPAN_DAYS]. `shortened` says the end was cut for length, which the user is told.
 */
export function customPeriodOf(
  from: string,
  until: string,
  min: string | null
): { period: ExportPeriod; shortened: boolean } {
  const begin = min && from < min ? min : from;
  const end = until < begin ? begin : until;
  const latest = plusDays(begin, MAX_SPAN_DAYS);
  const shortened = end > latest;
  return {
    period: { kind: null, from: begin, until: shortened ? latest : end },
    shortened,
  };
}

/**
 * The period of the url (`?period=quarter&from=2026-10-01`, or `?period=custom&from=…&until=…`), else the art
 * stored with the choice (the current period of it), else this quarter. One ending before `min` (an old link of
 * a user who may not look into previous years) is the current period of the art instead.
 */
export function initialExportPeriod(
  params: URLSearchParams,
  storedKind: string | null | undefined,
  kinds: readonly PeriodKind[],
  min: string | null,
  ctx: FormatContext
): ExportPeriod {
  const from = params.get("from");
  const until = params.get("until");
  // Only days that exist: `2026-13-01` has the shape, but the backend can't read it.
  if (
    params.get("period") === CUSTOM_PERIOD_KIND &&
    isIsoDate(from) &&
    isIsoDate(until) &&
    from <= until &&
    (!min || until >= min)
  ) {
    return customPeriodOf(from, until, min).period;
  }
  const offered = (id: string | null | undefined): PeriodKind | null => {
    const kind = periodKindOf(id);
    return kind && kinds.includes(kind) ? kind : null;
  };
  const kind = offered(params.get("period")) ?? offered(storedKind) ?? kinds[0];
  const anchor = isIsoDate(from)
    ? kind.beginOf(from, ctx)
    : currentAnchorOf(kind, ctx);
  return clampToMin(periodOfKind(kind, anchor, ctx), min, ctx);
}

/** The current period of the art instead of one ending before `min`; a free range is kept (see [customPeriodOf]). */
export function clampToMin(
  period: ExportPeriod,
  min: string | null,
  ctx: FormatContext
): ExportPeriod {
  if (!period.kind || !min || period.until >= min) return period;
  return periodOfKind(period.kind, currentAnchorOf(period.kind, ctx), ctx);
}

/**
 * A free range paged by the days it spans (what the arrows beside it do), or null when that would leave nothing
 * to see: a range ending before `min` stays where it is.
 */
export function stepCustomPeriod(
  period: ExportPeriod,
  steps: number,
  min: string | null,
  ctx: FormatContext
): ExportPeriod | null {
  const shifted = shiftBounds(period.from, period.until, null, steps, ctx);
  if (!shifted || (min && shifted.to < min)) return null;
  return customPeriodOf(shifted.from, shifted.to, min).period;
}
