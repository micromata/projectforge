import type { NumberSegment } from "@/lib/form/number-segments";

/**
 * The four parts a cost 1 number is made of, with the ranges Wicket's edit form enforces
 * (`Kost1EditForm.init`: `RequiredMinMaxNumberField` 0-9, 0-999, 0-99, 0-99) and the display widths of
 * its converters — `IntegerConverter(3)`/`(2)` on the last three, none on the nummernkreis, which is
 * why only those three are zero-padded (see `displaySegment`).
 *
 * These ranges are the one rule that cannot come from the generated metadata: `@Column(length = 3)`
 * is a digit count rather than a `max = 999`, and the generator drops it for non-strings anyway. So
 * they are declared once, here, and both the form field and the Zod schema read this array — the
 * authority remains `Kost1Dao.verifyKost`, whose refusal comes back as an HTTP 406.
 */
export const KOST1_SEGMENTS = [
  { name: "nummernkreis", min: 0, max: 9, digits: 1 },
  { name: "bereich", min: 0, max: 999, digits: 3 },
  { name: "teilbereich", min: 0, max: 99, digits: 2 },
  { name: "endziffer", min: 0, max: 99, digits: 2 },
] as const;

/**
 * The segments with their accessible names, for [SegmentedNumberField].
 *
 * @param label the label of one part, from the entity's own `@PropertyInfo` keys
 *   (`fibu.kost1.nummernkreis`, …) — never a text invented here.
 */
export function kost1Segments(
  label: (name: string) => string
): NumberSegment[] {
  return KOST1_SEGMENTS.map((segment) => ({
    ...segment,
    label: label(segment.name),
  }));
}
