/** How far the paid amount may sit from the gross sum before it is worth pointing out. */
const TOLERANCE = 0.1;

/**
 * The gross sum a paid amount misses by more than [TOLERANCE] — or null while there is nothing to say.
 *
 * What this is for is the keying error: a digit too many, two digits swapped. Those miss the gross sum by
 * a factor, while everything an invoice is legitimately paid with sits near it — a discount of two or
 * three percent is nowhere near a tenth, and only a discount larger than the tolerance itself would be
 * mentioned here.
 *
 * The gross sum **with** discount, which the recalculated sums also carry, is deliberately not a second
 * yardstick: `RechnungCalculator.calculateGrossSumWithDiscount` answers the *paid amount* as soon as one
 * is entered, so comparing against it would compare the typed amount with itself and never say anything.
 *
 * Silent whenever the answer would be a guess, the same way `useKost2Check` holds back while it doesn't
 * know: a warning has to be earned. No amount entered, the sums not back from the server yet, or an
 * invoice without a gross sum at all (no positions) — nothing to compare, nothing said.
 *
 * Returns the sum rather than a boolean because it is the number the message names: "differs from the
 * gross amount (2.394,00 €)" is checkable, "differs from the gross amount" leaves the reader to look it
 * up.
 */
export function deviatingGrossSum(
  zahlBetrag: number | null | undefined,
  sums: { grossSum?: number | null } | undefined
): number | null {
  const grossSum = sums?.grossSum;
  if (zahlBetrag == null || zahlBetrag === 0) return null;
  // A negative sum is no yardstick either: the relative distance to it would flip its sign.
  if (grossSum == null || grossSum <= 0) return null;
  const deviation = Math.abs(zahlBetrag - grossSum) / grossSum;
  // `<=`: exactly ten percent off is still within what was allowed. The epsilon is the rounding slack a
  // percentage always needs — 10 % of 33,33 is not representable, and a difference in the sixteenth
  // digit is not a deviation anybody entered.
  return deviation <= TOLERANCE + 1e-9 ? null : grossSum;
}
