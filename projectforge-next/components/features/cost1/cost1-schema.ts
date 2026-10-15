import { z } from "zod";
import { KOST1_METADATA } from "@/lib/metadata/kost1.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { KOST1_SEGMENTS } from "./cost-number-segments";

/**
 * Every rule below — mandatory, maximum length, the constants of the status enum — comes from Kost1DO
 * through `lib/metadata/kost1.generated.ts`. The only thing this file adds is the range of each part
 * of the number, which the metadata cannot carry (see cost-number-segments.ts).
 */
const m = fromMetadata(KOST1_METADATA);

/** The bounds of one part, read from the same array the input boxes are built from. */
function range(name: (typeof KOST1_SEGMENTS)[number]["name"]) {
  const { min, max } = KOST1_SEGMENTS.find((s) => s.name === name)!;
  return { min, max };
}

/**
 * Which fields the form has mirrors org.projectforge.rest.dto.Kost1 — a hand-written decision,
 * because the DTO has neither the field set nor the names of the DO. What each field *allows* is not.
 *
 * `formattedNumber` is deliberately absent: Kost1DO computes it from the four parts and its getter has
 * no backing field, so a value sent back would be dropped anyway (see `Kost1.copyTo`).
 */
export const cost1Schema = z.object({
  // null while the cost unit is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  nummernkreis: m.intField("nummernkreis", range("nummernkreis")),
  bereich: m.intField("bereich", range("bereich")),
  teilbereich: m.intField("teilbereich", range("teilbereich")),
  endziffer: m.intField("endziffer", range("endziffer")),
  kostentraegerStatus: m.enumField("kostentraegerStatus"),
  description: m.nullableString("description"),
  created: m.nullableString("created"),
});

export type Cost1Values = z.infer<typeof cost1Schema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const COST1_FIELDS = Object.keys(
  cost1Schema.shape
) as readonly (keyof Cost1Values)[];
