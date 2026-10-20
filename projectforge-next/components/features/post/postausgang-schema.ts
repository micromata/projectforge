import { z } from "zod";
import { POSTAUSGANG_METADATA } from "@/lib/metadata/postausgang.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * The rules PostausgangDO carries — date, type, receiver and content mandatory, the maximum lengths — come
 * from `lib/metadata/postausgang.generated.ts` through [fromMetadata]. That the date is not in the future
 * is checked by the backend (PostausgangEntityRest.validate).
 *
 * Which fields the form has mirrors PostausgangDO: a save posts these values *as* the entity, so a field
 * left out here would reach the backend as null and overwrite the stored one.
 */
const m = fromMetadata(POSTAUSGANG_METADATA);

export const postausgangSchema = z.object({
  // null while the entry is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  datum: m.instantField("datum"),
  type: m.enumField("type"),
  empfaenger: m.requiredString("empfaenger"),
  person: m.nullableString("person"),
  inhalt: m.requiredString("inhalt"),
  absender: m.nullableString("absender"),
  bemerkung: m.nullableString("bemerkung"),
});

export type PostausgangValues = z.infer<typeof postausgangSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const POSTAUSGANG_FIELDS = Object.keys(
  postausgangSchema.shape
) as readonly (keyof PostausgangValues)[];
