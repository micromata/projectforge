import {
  isCustomPeriod,
  periodKindOf,
  type Period,
  type PeriodKind,
} from "@/lib/date-period";
import { boundsOfPeriod, periodOfBounds } from "@/lib/date-period-bounds";
import {
  instantBoundsOfPeriod,
  periodOfInstantBounds,
} from "@/lib/date-period-instant";
import type { FormatContext } from "@/lib/format";
import type { MagicFilterEntryValue } from "@/lib/rs/types";
import { zonedPartsOf } from "@/lib/user-zone";
import type { FilterValues } from "./filter-value";

/**
 * The period a filter value is in, and bringing a stored one up to today.
 *
 * Most arts can be read straight off the two bounds (`periodOfBounds`), but one cannot: "Jahr bis heute"
 * ends on a day that is a different one tomorrow, so a range that happens to end today is no evidence it
 * was meant. That art therefore travels *with the filter entry* (`MagicFilterEntry.Value.periodKind`, a
 * plain string on the wire) and is read back from there.
 *
 * Which also settles what "bis heute" means on the next visit: it is refreshed when the stored filter
 * seeds the list (see [useListFilters]), so a filter left as 01.11.2025–22.08.2026 shows
 * 01.11.2025–23.08.2026 tomorrow instead of the frozen bounds every other quick access leaves behind.
 */

/** The art the value remembers, or null — for a range the bounds alone cannot tell. */
function storedPeriod(
  value: MagicFilterEntryValue | undefined,
  anchor: string | undefined,
  kinds: readonly PeriodKind[],
  ctx: FormatContext
): Period | null {
  const kind = periodKindOf(value?.periodKind);
  // Only what this field actually offers: a filter stored while another set of arts was on the field
  // must not resurrect one the user can neither see nor unset.
  if (!kind || !anchor || !kinds.includes(kind)) return null;
  try {
    return { kind, anchor: kind.beginOf(anchor, ctx) };
  } catch {
    return null;
  }
}

/**
 * The period of a DATE filter value: the art it remembers, else the one its two dates are.
 *
 * A value the user set to "Eigener Zeitraum" ([CUSTOM_PERIOD_KIND]) is no period at all, however whole a
 * calendar month its two dates may happen to be: the free-range marker exists precisely to stop that
 * read-back, so the begin can then be retyped to a non-first day without being snapped.
 */
export function periodOfDateValue(
  value: MagicFilterEntryValue | undefined,
  kinds: readonly PeriodKind[],
  ctx: FormatContext
): Period | null {
  if (isCustomPeriod(value?.periodKind)) return null;
  return (
    storedPeriod(value, value?.from, kinds, ctx) ??
    periodOfBounds(value?.from, value?.to, kinds, ctx)
  );
}

/** The same for a TIMESTAMP filter value, whose bounds are instants in the user's zone. */
export function periodOfInstantValue(
  value: MagicFilterEntryValue | undefined,
  kinds: readonly PeriodKind[],
  ctx: FormatContext
): Period | null {
  if (isCustomPeriod(value?.periodKind)) return null;
  return (
    storedPeriod(value, zonedPartsOf(value?.from, ctx)?.date, kinds, ctx) ??
    periodOfInstantBounds(value?.from, value?.to, kinds, ctx)
  );
}

/** The free result of a hand-typed bound: the art gone, and undefined once no bound is left. */
function freeEdit(
  value: MagicFilterEntryValue | undefined,
  part: "from" | "to",
  raw: string | null
): MagicFilterEntryValue | undefined {
  const merged = { ...value, periodKind: undefined, [part]: raw ?? undefined };
  return merged.from || merged.to ? merged : undefined;
}

/**
 * The value after a DATE bound was typed by hand.
 *
 * The end frees the range — the two dates are the user's again, and "bis heute" must not drag the other
 * end along tomorrow. A begin typed while an art is in effect keeps it instead and re-anchors the whole
 * period on that begin (`boundsOfPeriod`), so the end follows: a term measured off the new begin, "Jahr
 * bis heute" ending today again, and the calendar month snapped to the first of the begin's month — the
 * one asymmetry the stepper's [RangeField] `onSelect` does not have.
 */
export function editedDateValue(
  value: MagicFilterEntryValue | undefined,
  part: "from" | "to",
  raw: string | null,
  kinds: readonly PeriodKind[],
  ctx: FormatContext
): MagicFilterEntryValue | undefined {
  if (part === "from" && raw) {
    const period = periodOfDateValue(value, kinds, ctx);
    if (period) {
      try {
        return {
          ...boundsOfPeriod(period.kind, raw, ctx),
          periodKind: period.kind.id,
        };
      } catch {
        // A begin this art cannot compute from: fall through to the free range below.
      }
    }
  }
  return freeEdit(value, part, raw);
}

/** The same for a TIMESTAMP filter, whose begin is an instant and re-anchors on its date in the zone. */
export function editedInstantValue(
  value: MagicFilterEntryValue | undefined,
  part: "from" | "to",
  raw: string | null,
  kinds: readonly PeriodKind[],
  ctx: FormatContext
): MagicFilterEntryValue | undefined {
  if (part === "from" && raw) {
    const period = periodOfInstantValue(value, kinds, ctx);
    const anchor = zonedPartsOf(raw, ctx)?.date;
    if (period && anchor) {
      try {
        const bounds = instantBoundsOfPeriod(period.kind, anchor, ctx);
        if (bounds) return { ...bounds, periodKind: period.kind.id };
      } catch {
        // A begin this art cannot compute from: fall through to the free range below.
      }
    }
  }
  return freeEdit(value, part, raw);
}

/** Whether a value's bounds are instants (a time of day) rather than plain dates — an `hh:mm` says so. */
function isInstantValue(value: MagicFilterEntryValue | undefined): boolean {
  return !!value?.from?.includes("T") || !!value?.to?.includes("T");
}

/**
 * The period a filter pill can page — non-null exactly when the range is a nameable art (a stored
 * `periodKind`, or one the two bounds are), so the pill shows its arrows only when there is something to
 * page. DATE or TIMESTAMP is told from the bounds themselves, as [refreshedPeriodValues] does.
 */
export function pageablePeriodOf(
  value: MagicFilterEntryValue | undefined,
  kinds: readonly PeriodKind[],
  ctx: FormatContext
): Period | null {
  return isInstantValue(value)
    ? periodOfInstantValue(value, kinds, ctx)
    : periodOfDateValue(value, kinds, ctx);
}

/**
 * The value one period `steps` away, or null when there is nothing to page — the same shift the popover
 * stepper does ([RangeField.onSelect]), so paging from the pill and from the popover agree. The art is
 * kept on the value (`periodKind`), since one of the arts cannot be read back off the two dates.
 */
export function steppedPeriodValue(
  value: MagicFilterEntryValue | undefined,
  steps: number,
  kinds: readonly PeriodKind[],
  ctx: FormatContext
): MagicFilterEntryValue | null {
  const period = pageablePeriodOf(value, kinds, ctx);
  if (!period) return null;
  try {
    const anchor = period.kind.shift(period.anchor, steps, ctx);
    const bounds = isInstantValue(value)
      ? instantBoundsOfPeriod(period.kind, anchor, ctx)
      : boundsOfPeriod(period.kind, anchor, ctx);
    if (!bounds) return null;
    return { ...value, ...bounds, periodKind: period.kind.id };
  } catch {
    // A bound this cannot compute from — the value stays as it is, which is the period still on screen.
    return null;
  }
}

/**
 * Every stored value whose art moves with the calendar, recomputed for today.
 *
 * Whether the bounds are dates or instants is read off the value itself rather than off the field's
 * metadata: the two are unmistakable (`2026-08-22` against `2026-08-22T08:12:34.000Z`), and this runs
 * while the list is being set up, before the elements are known.
 */
export function refreshedPeriodValues(
  values: FilterValues,
  ctx: FormatContext
): FilterValues {
  const refreshed: FilterValues = {};
  for (const [field, value] of Object.entries(values)) {
    const kind = periodKindOf(value.periodKind);
    refreshed[field] =
      kind?.dependsOnToday && value.from
        ? { ...value, ...boundsOf(value.from, kind, ctx) }
        : value;
  }
  return refreshed;
}

function boundsOf(
  from: string,
  kind: PeriodKind,
  ctx: FormatContext
): { from: string; to: string } | undefined {
  try {
    return from.includes("T")
      ? (instantBoundsOfPeriod(
          kind,
          zonedPartsOf(from, ctx)?.date ?? "",
          ctx
        ) ?? undefined)
      : boundsOfPeriod(kind, from, ctx);
  } catch {
    // A bound the backend stored in a shape this cannot read: the value stays as it came, which is the
    // period the user last saw.
    return undefined;
  }
}
