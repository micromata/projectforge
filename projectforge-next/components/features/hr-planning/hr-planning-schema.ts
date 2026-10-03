import { z } from "zod";
import { HR_PLANNING_METADATA } from "@/lib/metadata/hr-planning.generated";
import { HR_PLANNING_ENTRY_METADATA } from "@/lib/metadata/hr-planning-entry.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Mandatory, maximum length and the enum values come from `HRPlanningDO` and `HRPlanningEntryDO` through
 * `lib/metadata/hr-planning*.generated.ts`. Which fields the form has mirrors
 * org.projectforge.rest.dto.HRPlanning / HRPlanningEntry.
 */
const m = fromMetadata(HR_PLANNING_METADATA);
const e = fromMetadata(HR_PLANNING_ENTRY_METADATA);

/** A referenced user or project, as the DTO carries it: the id is what `copyTo` stores. */
const entityRef = z
  .looseObject({ id: z.number(), displayName: z.string().optional() })
  .nullable();

/**
 * One entry of the week. A project *or* a status, never both and never neither — a rule of the entry
 * as a whole, checked by `HRPlanningEntityRest.validate` and reported at the project field.
 */
export const hrPlanningEntrySchema = z.object({
  // null while the entry is new; `deleted` is posted rather than the entry left out, which would remove
  // it physically (see HRPlanning.copyTo).
  id: z.number().nullable(),
  deleted: z.boolean(),
  status: e.enumField("status"),
  /** No metadata: `ProjektDO` has no UIDataType, so the project is absent from the generated file. */
  projekt: entityRef,
  priority: e.enumField("priority"),
  probability: e.intField("probability"),
  unassignedHours: e.decimalField("unassignedHours"),
  mondayHours: e.decimalField("mondayHours"),
  tuesdayHours: e.decimalField("tuesdayHours"),
  wednesdayHours: e.decimalField("wednesdayHours"),
  thursdayHours: e.decimalField("thursdayHours"),
  fridayHours: e.decimalField("fridayHours"),
  weekendHours: e.decimalField("weekendHours"),
  description: e.nullableString("description"),
});

export const hrPlanningSchema = z.object({
  // null while the planning is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  user: m.entityField("user"),
  week: m.requiredString("week"),
  entries: z.array(hrPlanningEntrySchema),
  created: m.nullableString("created"),
});

export type HRPlanningValues = z.infer<typeof hrPlanningSchema>;
export type HRPlanningEntryValues = z.infer<typeof hrPlanningEntrySchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors). The nested paths of the entries are matched by the array's name.
 */
export const HR_PLANNING_FIELDS = Object.keys(
  hrPlanningSchema.shape
) as readonly (keyof HRPlanningValues)[];

/** The nested collection of the form (see EditDef.arrayFieldNames). */
export const HR_PLANNING_ARRAY_FIELDS = ["entries"] as const;
