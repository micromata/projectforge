import type { FormatContext } from "./format";
import { PERIOD_KINDS, TERM_KINDS } from "./date-period-kinds";

/**
 * What a date period is: a begin plus *either* an end date *or* an art — one of [PERIOD_KINDS].
 *
 * Give both dates and the period is exactly those two days. Give a begin and a kind and the end
 * follows from it, and the arrows beside the boxes page the whole period in that kind's steps. Every
 * case a period is entered in on any surface of the app is one of the two, which is why this is one
 * model and not the two it used to be (a calendar section for the filters, a term for a
 * Leistungszeitraum).
 *
 * A period lives as `yyyy-MM-dd` strings, like everything since ./date-parse.ts — that is how a
 * `LocalDate` travels over the wire, and no `Date` is ever a value here. The arithmetic is in
 * ./date-period-math.ts and zone-free on purpose; what a time zone does to a period belongs in
 * ./date-period-instant.ts, and the queries over a period in ./date-period-bounds.ts.
 *
 * Not to be confused with `components/data-table/history-interval-presets.ts`: those are rolling
 * windows that always end at "now" and set absolute bounds once. A `yearToDate` period keeps its kind
 * and is therefore still up to today the next time the list is opened.
 */

export type PeriodKindId =
  | "week"
  | "month"
  | "yearToDate"
  | "termWeek"
  | "termMonth"
  | "termThreeMonths"
  | "termYear";

/**
 * One art a period can be given as — how its end follows from its begin, and what the arrows step by.
 *
 * Every kind carries its texts as spelled-out `…Key` properties rather than letting a component build
 * them (`` t(`…select${id}`) ``): a key assembled at runtime is invisible to `NextI18nKeyScanner`, so
 * it would never reach `messages/generated.*.json` and the button would show the raw key. Spelled out
 * it is found, and a typo is reported by the generator.
 */
export interface PeriodKind {
  id: PeriodKindId;
  /** Name of the kind itself, for the picker that offers them. */
  labelKey: string;
  /**
   * The same name in one or two characters ("3M", "J→"), for the trigger that has to fit beside two
   * date boxes. The list of choices shows [labelKey], so the short form never stands alone.
   */
  shortLabelKey: string;
  /**
   * Fills the `{arg0}` of a counted name ("3 Monate"); absent where the key names the unit alone. The
   * same count for both texts — they say the same thing.
   */
  labelArg?: number;
  tooltipPreviousKey: string;
  tooltipNextKey: string;
  /**
   * Names the current period of this art, which is what picking the art again in the stepper sets.
   * Absent where there is no such thing: "die aktuelle Woche" is nothing one does to an agreed period
   * of performance, and then picking it again does nothing.
   */
  tooltipCurrentKey?: string;
  /** First day of the period `iso` belongs to — the anchor, snapped where the kind snaps. */
  beginOf(iso: string, ctx: FormatContext): string;
  /** Last day of it, given its anchor. */
  endOf(iso: string, ctx: FormatContext): string;
  /** The anchor of the period `steps` steps away. */
  shift(iso: string, steps: number, ctx: FormatContext): string;
  /**
   * The anchor to start from with both boxes still empty. Absent where `beginOf(today)` is the answer,
   * which it is for every kind that snaps.
   */
  currentAnchor?(ctx: FormatContext): string;
  /**
   * Set where the end moves with the calendar (`yearToDate`). Such a kind is never *derived* from a
   * pair of dates — it is remembered instead, so a hand-typed range that happens to end today cannot
   * start paging by years. See `periodOfBounds`.
   */
  dependsOnToday?: boolean;
}

/** A period in effect: the art it is given as, and the day it begins on. */
export interface Period {
  kind: PeriodKind;
  anchor: string;
}

export { PERIOD_KINDS };

/**
 * The marker a filter value carries once the user released the art for a free range ("Eigener Zeitraum"):
 * the two dates are kept, but no art is read back off them, so a range that happens to be a whole calendar
 * month is no longer snapped when its begin is retyped. It is deliberately no [PeriodKindId] — `periodKindOf`
 * returns null for it, which is what makes the derivation fall away (see `periodOfDateValue`).
 */
export const CUSTOM_PERIOD_KIND = "custom";

/** Whether a stored `periodKind` is the free-range marker rather than one of the arts. */
export function isCustomPeriod(periodKind: string | null | undefined): boolean {
  return periodKind === CUSTOM_PERIOD_KIND;
}

/** The ids of the term kinds, for a field that offers all of them — a period of performance. */
export const TERM_KIND_IDS: readonly PeriodKindId[] = TERM_KINDS.map(
  (kind) => kind.id
);

/**
 * The kinds named, in the order [PERIOD_KINDS] has them; an unknown id is dropped.
 *
 * The order of the *offered* list is what decides an ambiguous read-back (see `periodOfBounds`), so a
 * caller lists what makes sense on its surface and gets them in one canonical order.
 */
export function periodKindsOf(
  ids: readonly PeriodKindId[] | undefined
): PeriodKind[] {
  if (!ids?.length) return [];
  return PERIOD_KINDS.filter((kind) => ids.includes(kind.id));
}

/** The one kind with that id, or null — for a selection held as an id, as a stored filter holds it. */
export function periodKindOf(
  id: PeriodKindId | string | null | undefined
): PeriodKind | null {
  return PERIOD_KINDS.find((kind) => kind.id === id) ?? null;
}
