import { z } from "zod";
import { VACATION_METADATA } from "@/lib/metadata/vacation.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { REQUIRED } from "@/lib/validation/markers";

/**
 * Mandatory (the dates, the status), maximum length and the enum constants come from VacationDO through
 * `lib/metadata/vacation.generated.ts`. Which fields the form has mirrors org.projectforge.rest.dto.Vacation.
 *
 * Every property of the entry is part of it, rendered or not: a save posts these values *as* the DTO, so a
 * field left out here would reach `Vacation.copyTo` as null and overwrite the stored one.
 */
const m = fromMetadata(VACATION_METADATA);

/** A further replacement, as `Vacation.otherReplacements` carries it: the id is what `copyTo` stores. */
const employeeRef = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

export const vacationSchema = z.object({
  // null while the entry is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  employee: m.entityField("employee"),
  startDate: m.instantField("startDate"),
  endDate: m.instantField("endDate"),
  halfDayBegin: m.booleanField("halfDayBegin"),
  halfDayEnd: m.booleanField("halfDayEnd"),
  // Mandatory as on the React form, but not in the metadata: `required` of `@PropertyInfo` doesn't reach a
  // reference (see VacationEntityRest.validate, which checks it, too).
  manager: m.entityField("manager").refine((v): boolean => v != null, REQUIRED),
  replacement: m
    .entityField("replacement")
    .refine((v): boolean => v != null, REQUIRED),
  /** No metadata: a collection of references. An empty list rather than null, which is what the picker holds. */
  otherReplacements: z.array(employeeRef),
  status: m.enumField("status"),
  special: m.booleanField("special"),
  comment: m.nullableString("comment"),
});

export type VacationValues = z.infer<typeof vacationSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const VACATION_FIELDS = Object.keys(
  vacationSchema.shape
) as readonly (keyof VacationValues)[];
