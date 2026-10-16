import { z } from "zod";
import { AVAILABILITY_METADATA } from "@/lib/metadata/availability.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { REQUIRED } from "@/lib/validation/markers";

/**
 * Mandatory (the dates), maximum length and the enum constants come from AvailabilityDO through
 * `lib/metadata/availability.generated.ts`. Which fields the form has mirrors org.projectforge.rest.dto.Availability.
 *
 * Every property of the entry is part of it, rendered or not: a save posts these values *as* the DTO, so a field
 * left out here would reach `Availability.copyTo` as null and overwrite the stored one.
 */
const m = fromMetadata(AVAILABILITY_METADATA);

export const availabilitySchema = z.object({
  // null while the entry is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  // Mandatory, but not in the metadata: `required` of `@PropertyInfo` doesn't reach a reference (see
  // AvailabilityEntityRest.validate, which checks it, too).
  user: m.entityField("user").refine((v): boolean => v != null, REQUIRED),
  startDate: m.instantField("startDate"),
  endDate: m.instantField("endDate"),
  type: m.nullableString("type"),
  // Mandatory as on the React form, though the column may be null.
  status: m.enumField("status").refine((v): boolean => v != null, REQUIRED),
  location: m.enumField("location"),
  comment: m.nullableString("comment"),
  internalComment: m.nullableString("internalComment"),
});

export type AvailabilityValues = z.infer<typeof availabilitySchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders (see
 * applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const AVAILABILITY_FIELDS = Object.keys(
  availabilitySchema.shape
) as readonly (keyof AvailabilityValues)[];
