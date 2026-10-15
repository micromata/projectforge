import type { FormatContext } from "./format";
import type { Period, PeriodKind } from "./date-period";
import { inclusiveDays, plusDays } from "./date-period-math";
import { todayOf } from "./user-zone";

/**
 * The questions asked about a period (see ./date-period.ts): what two dates is it, which kind are these
 * two dates, and where does it land after n clicks on an arrow.
 *
 * Every one of them goes through the kind's own `beginOf`/`endOf`/`shift`, so what is read back can
 * never disagree with what was written, whatever the arithmetic does at the end of a month.
 */

/** A whole period as the two dates it spans. */
export function boundsOfPeriod(
  kind: PeriodKind,
  anchor: string,
  ctx: FormatContext
): { from: string; to: string } {
  return { from: kind.beginOf(anchor, ctx), to: kind.endOf(anchor, ctx) };
}

/** The end of the period beginning on `begin`, or null where that is no date — for a box being typed in. */
export function endOfPeriod(
  begin: string | null | undefined,
  kind: PeriodKind,
  ctx: FormatContext
): string | null {
  if (!begin) return null;
  try {
    return kind.endOf(kind.beginOf(begin, ctx), ctx);
  } catch {
    return null;
  }
}

/**
 * The period `steps` steps on, as its two ends — or null when there is nothing to move (an end missing,
 * or one of them no date). This is what the paging arrows beside the two boxes do.
 *
 * One rule, two ways of measuring a step. With a kind in effect the kind decides: from a "Monat" on
 * 01.03.–31.03. one click has to reach 01.04.–30.04., not a period shifted by that month's 31 days, and
 * from a "Jahr bis heute" it has to reach the same window a year back. With no kind it is the number of
 * days the two ends span, so a period entered by hand moves by exactly as much as it covers.
 *
 * Not reversible at the end of a month — 31.01. one on is 28.02., one back from there is 28.01. That is
 * `LocalDate.plusMonths`, the rule of the kinds themselves; a stepper that remembered where it came from
 * would show a period the two boxes do not.
 */
export function shiftBounds(
  from: string | null | undefined,
  to: string | null | undefined,
  kind: PeriodKind | null,
  steps: number,
  ctx: FormatContext
): { from: string; to: string } | null {
  if (!from || !to) return null;
  try {
    if (kind) {
      const anchor = kind.shift(kind.beginOf(from, ctx), steps, ctx);
      return { from: anchor, to: kind.endOf(anchor, ctx) };
    }
    const length = inclusiveDays(from, to);
    return {
      from: plusDays(from, length * steps),
      to: plusDays(to, length * steps),
    };
  } catch {
    // One of the two is not a date, so there is no period to move — a filter box can hold anything.
    return null;
  }
}

/**
 * Which period the two bounds are, or null when they are none.
 *
 * Both ends are needed: a half-open range is not a period, however it is written. The kinds are tried
 * in the order they were *offered*, and unlike before that order matters — 01.03.–31.03. is a whole
 * calendar month *and* a "Monat ab Beginn", so which name the same two dates get is the surface's
 * choice. That is harmless because both readings page the same way from an aligned period, and it is
 * the price of one model instead of two.
 *
 * Kinds whose end moves with the calendar are skipped entirely (`dependsOnToday`): a range ending today
 * is not evidence that "bis heute" was meant, and it would start paging by years the moment it was
 * typed. Such a kind comes only from what was stored (`MagicFilterEntry.Value.periodKind`).
 */
export function periodOfBounds(
  from: string | null | undefined,
  to: string | null | undefined,
  kinds: readonly PeriodKind[],
  ctx: FormatContext
): Period | null {
  if (!from || !to) return null;
  for (const kind of kinds) {
    if (kind.dependsOnToday) continue;
    try {
      const bounds = boundsOfPeriod(kind, from, ctx);
      if (bounds.from === from && bounds.to === to) {
        return { kind, anchor: bounds.from };
      }
    } catch {
      // Not an ISO date, so not a period either — and no other kind will make one of it.
      return null;
    }
  }
  return null;
}

/**
 * The period the arrows should page from, given bounds that are no whole period — as its anchor, or
 * null when neither bound is a date.
 *
 * The lower bound decides, and only in its absence the upper one: a range being filled in reads from
 * left to right, so the month the user just entered is the one the panel has to name. Paging from
 * "today" while a start date says otherwise would jump the range somewhere unrelated with one click.
 */
export function anchorOfBounds(
  // Undefined where quick access is switched off (`kinds[0]` of an empty list), which is a call the
  // caller shouldn't have to guard — the stepper renders nothing there anyway.
  kind: PeriodKind | undefined,
  from: string | null | undefined,
  to: string | null | undefined,
  ctx: FormatContext
): string | null {
  if (!kind) return null;
  for (const bound of [from, to]) {
    if (!bound) continue;
    try {
      return kind.beginOf(bound, ctx);
    } catch {
      // Not a date but something half-typed into a filter; the other bound may still be one.
    }
  }
  return null;
}

/**
 * The anchor of the period today falls in — where the arrows start from with both boxes still empty.
 *
 * A kind that snaps answers it itself, off the user's today (`todayOf`, i.e. `ctx.timeZone`: near
 * midnight an account set to another zone than the machine is on a different day, and then "der
 * aktuelle Monat" would be the wrong one). A kind that does not snap has to say what "the current one"
 * even means, and does so through `currentAnchor`.
 */
export function currentAnchorOf(kind: PeriodKind, ctx: FormatContext): string {
  const today = todayOf(ctx);
  return kind.currentAnchor?.(ctx) ?? kind.beginOf(today, ctx);
}
