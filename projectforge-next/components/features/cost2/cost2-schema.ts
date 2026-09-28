import { z } from "zod";
import { KOST2_METADATA } from "@/lib/metadata/kost2.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import {
  INTEGER,
  REQUIRED,
  maxMarker,
  minMarker,
} from "@/lib/validation/markers";
import { KOST2_SEGMENTS } from "./cost2-number-segments";

/**
 * The rules that Kost2DO carries — mandatory, maximum length, the constants of the status enum, the
 * bounds of `workFraction` — come from `lib/metadata/kost2.generated.ts` through [fromMetadata]. What
 * this file adds by hand are the four parts of the number: unlike cost1's, Kost2DO's parts have no
 * `@PropertyInfo`, so the metadata carries neither their bounds nor even the fields, and the last part
 * is the id of a `Kost2Art` (see cost2-number-segments.ts). `project` is a `ProjektDO`, which has no
 * `UIDataType` and so no metadata either — declared here like the invoice's references.
 */
const m = fromMetadata(KOST2_METADATA);

/** The DTO carries a project reference as `{id, displayName}`; only the id is written back. */
const entityRef = z
  .looseObject({ id: z.number(), displayName: z.string().optional() })
  .nullable();

/**
 * One part of the number. Reproduces [fromMetadata.intField] for a field the metadata cannot describe:
 * a whole number, mandatory (a part left empty must report "required", not silently save a different
 * number), bounded by the same array the input boxes are built from. The markers are the backend's, so
 * the wording matches the HTTP 406 the server would answer with.
 */
function segment(name: (typeof KOST2_SEGMENTS)[number]["name"]) {
  const { min, max } = KOST2_SEGMENTS.find((s) => s.name === name)!;
  return z
    .number()
    .nullable()
    .refine((v) => v == null || Number.isInteger(v), INTEGER)
    .refine((v): boolean => v != null, REQUIRED)
    .refine((v) => v == null || v >= min, minMarker(min))
    .refine((v) => v == null || v <= max, maxMarker(max));
}

/**
 * Which fields the form has mirrors org.projectforge.rest.dto.Kost2 — a hand-written decision. What
 * each field allows is not (see above).
 *
 * `formattedNumber`, `effectiveKostentraegerStatus` and the read side of `kost2Art`/`project` are
 * deliberately absent from what is *edited*: the number's four parts and `project` are the inputs, and
 * `Kost2.copyTo` derives the rest.
 */
export const cost2Schema = z.object({
  // null while the cost unit is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  nummernkreis: segment("nummernkreis"),
  bereich: segment("bereich"),
  teilbereich: segment("teilbereich"),
  // The Kost2Art's id (0-99); Kost2.copyTo resolves it to the reference the entity persists.
  endziffer: segment("endziffer"),
  project: entityRef,
  workFraction: m.decimalField("workFraction"),
  kostentraegerStatus: m.enumField("kostentraegerStatus"),
  description: m.nullableString("description"),
  comment: m.nullableString("comment"),
});

export type Cost2Values = z.infer<typeof cost2Schema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const COST2_FIELDS = Object.keys(
  cost2Schema.shape
) as readonly (keyof Cost2Values)[];
