import { z } from "zod";
import { KOST2_ART_METADATA } from "@/lib/metadata/kost2-art.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Every rule below — mandatory, maximum length — comes from Kost2ArtDO through
 * `lib/metadata/kost2-art.generated.ts`. Which fields the form has mirrors
 * org.projectforge.rest.dto.Kost2Art; what each field *allows* is not restated here.
 */
const m = fromMetadata(KOST2_ART_METADATA);

export const cost2TypeSchema = z.object({
  // The two-digit Nummer *is* the primary key, typed for a new type and never changed afterwards; null
  // only while the add form is still empty. Bounded to two digits on the input, not here — a saved value
  // outside 0..99 is the backend's own concern.
  id: z.number().int().nullable(),
  name: m.requiredString("name"),
  fakturiert: m.booleanField("fakturiert"),
  workFraction: m.decimalField("workFraction"),
  projektStandard: m.booleanField("projektStandard"),
  description: m.nullableString("description"),
});

export type Cost2TypeValues = z.infer<typeof cost2TypeSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const COST2_TYPE_FIELDS = Object.keys(
  cost2TypeSchema.shape
) as readonly (keyof Cost2TypeValues)[];
