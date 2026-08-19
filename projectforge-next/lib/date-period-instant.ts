import {
  anchorOfBounds,
  boundsOfPeriod,
  periodOfBounds,
  type Period,
  type PeriodUnit,
} from "./date-period";
import type { FormatContext } from "./format";
import {
  DEFAULT_FROM_TIME,
  DEFAULT_TO_TIME,
  zonedIsoOf,
  zonedPartsOf,
} from "./user-zone";

/**
 * A calendar period as the pair of instants a TIMESTAMP filter is made of.
 *
 * ./date-period.ts knows nothing about time zones on purpose, and a timestamp bound is nothing but a
 * zone applied to a wall clock — so the two meet here rather than in either of them.
 *
 * Each end is converted on its own, never as "the other end plus n days": within a single month the
 * zone's offset can change (Berlin in March 2026 is +01:00 on the 1st and +02:00 on the 31st), and an
 * offset applied to the wrong end is an hour out — near midnight, a whole day. [zonedIsoOf] resolves
 * each wall clock against the offset actually in force at it.
 *
 * The end is 23:59, not the following midnight, because that is what `DateTimeInput` writes for a
 * date typed into an upper bound (`DEFAULT_TO_TIME`). Generation and recognition therefore agree: a
 * period this module produced reads back as that period, and a bound the user edited by hand does
 * not — which is exactly the intent, since then no period is in effect any more.
 */

/** A whole period as the two instants bounding it, in the user's zone. */
export function instantBoundsOfPeriod(
  unit: PeriodUnit,
  anchor: string,
  ctx: FormatContext
): { from: string; to: string } | null {
  const bounds = boundsOfPeriod(unit, anchor, ctx);
  const from = zonedIsoOf(bounds.from, DEFAULT_FROM_TIME, ctx);
  const to = zonedIsoOf(bounds.to, DEFAULT_TO_TIME, ctx);
  return from && to ? { from, to } : null;
}

/**
 * Which whole period the two instants are, or null when they are not one.
 *
 * Both have to fall on the very edges of their days as the user reads them — 00:00 and 23:59 in
 * `ctx.timeZone`. A bound at 23:00, or at midnight of the following day, is not the end of a period
 * this module would ever write.
 */
export function periodOfInstantBounds(
  from: string | null | undefined,
  to: string | null | undefined,
  units: readonly PeriodUnit[],
  ctx: FormatContext
): Period | null {
  const begin = zonedPartsOf(from, ctx);
  const end = zonedPartsOf(to, ctx);
  if (begin?.time !== DEFAULT_FROM_TIME || end?.time !== DEFAULT_TO_TIME) {
    return null;
  }
  return periodOfBounds(begin.date, end.date, units, ctx);
}

/**
 * [anchorOfBounds] for a pair of instants: the period to page from, read as the *user's* day.
 *
 * An instant at 00:30 UTC is already the next day in Berlin, so taking the date out of the ISO string
 * would name the month before the one the input shows for the first hours of every month.
 */
export function anchorOfInstantBounds(
  unit: PeriodUnit | undefined,
  from: string | null | undefined,
  to: string | null | undefined,
  ctx: FormatContext
): string | null {
  return anchorOfBounds(
    unit,
    zonedPartsOf(from, ctx)?.date,
    zonedPartsOf(to, ctx)?.date,
    ctx
  );
}
