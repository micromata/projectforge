import { z } from "zod";
import { SKILL_ENTRY_METADATA } from "@/lib/metadata/skill-entry.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Mandatory and maximum length come from SkillEntryDO through `lib/metadata/skill-entry.generated.ts`.
 * Which fields the form has mirrors org.projectforge.rest.dto.SkillEntry.
 */
const m = fromMetadata(SKILL_ENTRY_METADATA);

/** The owner, as `SkillEntry.owner` carries it: the id is what `copyTo` stores. */
const userRef = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

/**
 * The owner and `created` are carried without being edited: a save posts these values *as* the DTO, so a
 * field left out here would reach `SkillEntry.copyTo` as null and overwrite the stored one. The ratings
 * run 0..3 (`SkillEntryDO.MAX_VAL_RATING`, `MAX_VAL_INTEREST`), which `SkillEntryDao` clamps to anyway.
 */
export const skillEntrySchema = z.object({
  // null while the skill is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  skill: m.requiredString("skill"),
  owner: userRef.nullable(),
  rating: m.intField("rating", { min: 0, max: 3 }),
  interest: m.intField("interest", { min: 0, max: 3 }),
  comment: m.nullableString("comment"),
  created: z.string().nullable(),
});

export type SkillEntryValues = z.infer<typeof skillEntrySchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const SKILL_ENTRY_FIELDS = Object.keys(
  skillEntrySchema.shape
) as readonly (keyof SkillEntryValues)[];
