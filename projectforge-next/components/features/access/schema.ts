import { z } from "zod";
import { GROUP_TASK_ACCESS_METADATA } from "@/lib/metadata/group-task-access.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { ACCESS_TYPES } from "./types";

/**
 * The rules for `recursive` and `description` come from GroupTaskAccessDO through
 * `lib/metadata/group-task-access.generated.ts`. Group, task and the permission matrix are hand-written:
 * the DO serializes group/task id-only (no metadata a select could read), and the matrix is a collection
 * no `@PropertyInfo` field describes.
 */
const m = fromMetadata(GROUP_TASK_ACCESS_METADATA);

/**
 * A referenced group or task, as the DTO carries it and the autocomplete binds it: the id is what
 * `GroupTaskAccess.copyTo` resolves the entity by. `looseObject` so the extra `name`/`title`/displayName
 * the backend sends ride along untouched.
 */
const entityRef = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

/** One row of the permission matrix — an access type with its four boolean flags. */
const accessEntrySchema = z.object({
  accessType: z.enum(ACCESS_TYPES),
  accessSelect: z.boolean(),
  accessInsert: z.boolean(),
  accessUpdate: z.boolean(),
  accessDelete: z.boolean(),
});

/**
 * Mirrors org.projectforge.rest.dto.GroupTaskAccess. Group and task are nullable here — a missing one is
 * refused server-side (the group/task pair is the entity's unique key), which keeps the one authority for
 * that rule in the backend.
 */
export const accessSchema = z.object({
  // null while the entry is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  group: entityRef.nullable(),
  task: entityRef.nullable(),
  recursive: m.booleanField("recursive"),
  description: m.nullableString("description"),
  accessEntries: z.array(accessEntrySchema),
});

export type AccessValues = z.infer<typeof accessSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const ACCESS_FIELDS = Object.keys(
  accessSchema.shape
) as readonly (keyof AccessValues)[];
