import type { NumberSegment } from "@/lib/form/number-segments";

/**
 * The four parts a cost 2 number is made of, with the ranges Wicket's edit form enforces
 * (`Kost2EditForm.init`: `MinMaxNumberField`/`RequiredMinMaxNumberField` 0-9, 0-999, 0-99, and the
 * last box 0-99 bound to `kost2Art.id`) and the display widths of its converters — the last three are
 * zero-padded, the nummernkreis is not (see `displaySegment`).
 *
 * The last part is the id of the `Kost2Art`, not a plain digit group: the DTO carries it flat as
 * `endziffer` and `Kost2.copyTo` resolves it back to the `Kost2ArtDO` reference the entity persists.
 * Mirrors `cost1`'s four boxes exactly — the difference is only what the last part means.
 *
 * These ranges are the one rule that cannot come from the generated metadata (`@Column(length = 3)` is
 * a digit count, not a `max`, and the generator drops it for non-strings), so they are declared once,
 * here, and both the form field and the Zod schema read this array — the authority remains the
 * entity's own check, whose refusal comes back as an HTTP 406.
 */
export const KOST2_SEGMENTS = [
  { name: "nummernkreis", min: 0, max: 9, digits: 1 },
  { name: "bereich", min: 0, max: 999, digits: 3 },
  { name: "teilbereich", min: 0, max: 99, digits: 2 },
  { name: "endziffer", min: 0, max: 99, digits: 2 },
] as const;

/**
 * The segments with their accessible names, for [SegmentedNumberField].
 *
 * @param label the label of one part — the first three from the generic cost keys
 *   (`fibu.kost1.nummernkreis`, …, which Kost2DO's parts have no `@PropertyInfo` of their own for),
 *   the last from `fibu.kost2.art` (the Kost2Art the box selects).
 */
export function kost2Segments(
  label: (name: string) => string
): NumberSegment[] {
  return KOST2_SEGMENTS.map((segment) => ({
    ...segment,
    label: label(segment.name),
  }));
}
