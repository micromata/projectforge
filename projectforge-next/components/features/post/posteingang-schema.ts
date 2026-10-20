import { z } from "zod";
import { POSTEINGANG_METADATA } from "@/lib/metadata/posteingang.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * The rules PosteingangDO carries — date, type, sender and content mandatory, the maximum lengths — come
 * from `lib/metadata/posteingang.generated.ts` through [fromMetadata]. That the date is not in the future
 * is checked by the backend (PosteingangEntityRest.validate).
 *
 * Which fields the form has mirrors PosteingangDO: a save posts these values *as* the entity, so a field
 * left out here would reach the backend as null and overwrite the stored one.
 */
const m = fromMetadata(POSTEINGANG_METADATA);

export const posteingangSchema = z.object({
  // null while the entry is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  datum: m.instantField("datum"),
  type: m.enumField("type"),
  absender: m.requiredString("absender"),
  person: m.nullableString("person"),
  inhalt: m.requiredString("inhalt"),
  bemerkung: m.nullableString("bemerkung"),
});

export type PosteingangValues = z.infer<typeof posteingangSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const POSTEINGANG_FIELDS = Object.keys(
  posteingangSchema.shape
) as readonly (keyof PosteingangValues)[];
