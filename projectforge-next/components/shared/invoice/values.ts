/**
 * The arithmetic a cost split and its positions are read by, shared by both invoice kinds.
 *
 * Entity-agnostic on purpose: a cost assignment (`KostZuweisungDO`) and how much of a position is left
 * to assign are the same on an outgoing and an incoming invoice, so the sums line, the share column and
 * the "what is still open" proposal all read the same numbers here. The entity-specific *empties* (with
 * their own default fields) stay per feature and delegate to these.
 *
 * Kept apart from any component so the rules can be asserted without a DOM (see
 * components/shared/invoice/*.test or the feature value tests).
 */

/** The least a row of a cost split has to carry for the arithmetic below. */
interface AssignmentLike {
  netto?: number | null;
  index?: number | null;
  deleted?: boolean;
}

/** The least a position has to carry for its number to be continued. */
interface PositionLike {
  number?: number | null;
}

/**
 * What of a position's net sum is not assigned to a cost unit yet — the amount a new row is proposed
 * with, and `null` where there is nothing to propose.
 *
 * Half server, half form on purpose. `netSum` is the server's (`RechnungCalculator` rounds a position
 * before it enters a sum, which is German law and not to be reimplemented here), while what is already
 * assigned is read from the form: the sums are debounced, so a row added right after an amount was typed
 * would otherwise be prefilled from a number that is no longer on screen. No rounding rule is duplicated
 * by this — it only adds up amounts the user entered.
 *
 * Deleted rows don't count, as they don't for `RechnungCalculator`. A remaining of zero yields `null`
 * rather than 0: an empty box is filled in, a `0,00 €` has to be cleared first. An over-assigned
 * position yields a negative amount, which is what Wicket proposes too — the row that is one too many
 * should read as one too many.
 */
export function remainingNet(
  netSum: number | null | undefined,
  assignments: readonly AssignmentLike[]
): number | null {
  if (netSum == null) return null;
  const assigned = assignments.reduce(
    (sum, entry) => (entry.deleted ? sum : sum + (entry.netto ?? 0)),
    0
  );
  // Rounded to the two digits an amount has: 2000 - 1900.1 is 99.90000000000009 in binary floating
  // point, and that is what would land in the box.
  const remaining = Number((netSum - assigned).toFixed(2));
  return remaining === 0 ? null : remaining;
}

/**
 * The share of its position one cost assignment carries, as a fraction — 0.5 for half of it.
 *
 * `null` where there is no share to state: an amount that is not filled in yet, or a position that sums
 * to nothing, which is also what keeps the division defined. Wicket's `Prozent` column leaves its cell
 * blank in exactly those two cases.
 *
 * Unrounded: what it is rounded to is the reader's business, not the ratio's (see
 * [formatPercentageDecimal], which the two places showing this pass 0 digits to).
 */
export function shareOfNetSum(
  netto: number | null | undefined,
  netSum: number | null | undefined
): number | null {
  if (netto == null || netto === 0 || netSum == null || netSum === 0)
    return null;
  return netto / netSum;
}

/**
 * The index the next cost assignment of a position gets: one past the highest that position holds,
 * deleted and stored rows included — `KostZuweisungDO`'s identity is `(index, owner)`, so reusing one
 * would merge a new row with a deleted row's past.
 *
 * **0-based**, unlike a position's number: an empty list starts at 0, which is what
 * `KostZuweisungDO.addKostZuweisung` does.
 */
export function nextKostZuweisungIndex(
  assignments: readonly AssignmentLike[]
): number {
  return assignments.length === 0
    ? 0
    : assignments.reduce((max, entry) => Math.max(max, entry.index ?? 0), 0) +
        1;
}

/**
 * The number the next position gets: one past the highest in the form, deleted and stored rows
 * included — the number is what `UNIQUE(rechnung_fk, number)` and the collection handler identify a
 * position by, so reusing one would collide with a row that is still in the database.
 */
export function nextPositionNumber(positions: readonly PositionLike[]): number {
  return positions.reduce((max, pos) => Math.max(max, pos.number ?? 0), 0) + 1;
}
