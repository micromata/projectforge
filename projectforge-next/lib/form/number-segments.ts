/**
 * The pure rules of a number made of several bounded parts — a cost number, for instance.
 *
 * Kept out of the component so they can be tested without a DOM (see vitest.config.mts): what is worth
 * asserting here is the arithmetic and the parsing, not the focus handling, which Playwright covers.
 */

export interface NumberSegment {
  /** Form value of this box, e.g. "nummernkreis" — one field of the DTO, not part of a composite. */
  name: string;
  /** Accessible name of the box, e.g. "Bereich". */
  label: string;
  min: number;
  max: number;
  /** Display width; more than one digit is zero-padded (see [displaySegment]). */
  digits: number;
}

/**
 * How a segment's value is shown.
 *
 * Padding is display only, and only where the segment is wider than one digit — Wicket pads
 * bereich/teilbereich/endziffer of a cost number, but not the single-digit nummernkreis.
 */
export function displaySegment(value: number | null, digits: number): string {
  if (value == null) return "";
  return digits > 1 ? String(value).padStart(digits, "0") : String(value);
}

/** Keeps only digits and no more of them than the segment is wide. */
export function segmentDigits(text: string, digits: number): string {
  return text.replace(/\D/g, "").slice(0, digits);
}

/** The value a box holds after typing, where empty is "no value" rather than 0. */
export function segmentValue(text: string): number | null {
  const digits = text.replace(/\D/g, "");
  return digits === "" ? null : Number(digits);
}

/** One step up or down, never outside the segment's range. */
export function stepSegment(
  value: number | null,
  segment: NumberSegment,
  direction: 1 | -1
): number {
  const stepped = (value ?? segment.min) + direction;
  return Math.min(segment.max, Math.max(segment.min, stepped));
}

/**
 * Splits a whole number pasted into any box, e.g. "6.100.01.02" into its four parts.
 *
 * Splitting at everything that isn't a digit rather than at the separator: the number is copied from
 * places that write it differently ("6-100-01-02", "6 100 01 02"). Parts beyond the last segment are
 * dropped, and a short paste leaves the remaining boxes untouched.
 */
export function splitPastedSegments(
  text: string,
  segments: NumberSegment[]
): Map<string, number> {
  const parts = text.split(/\D+/).filter((p) => p.length > 0);
  const values = new Map<string, number>();
  segments.forEach((segment, i) => {
    const part = parts[i];
    if (part === undefined) return;
    values.set(segment.name, Number(part.slice(0, segment.digits)));
  });
  return values;
}
