import { z } from "zod";
import { TO_DO_METADATA } from "@/lib/metadata/to-do.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { REQUIRED } from "@/lib/validation/markers";

/**
 * Mandatory (the subject), maximum length and the enum constants come from ToDoDO through
 * `lib/metadata/to-do.generated.ts`. Which fields the form has mirrors
 * org.projectforge.plugins.todo.dto.ToDo.
 */
const m = fromMetadata(TO_DO_METADATA);

export const toDoSchema = z.object({
  // null while the to-do is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  subject: m.requiredString("subject"),
  type: m.enumField("type"),
  status: m.enumField("status"),
  priority: m.enumField("priority"),
  dueDate: m.instantField("dueDate"),
  // Mandatory as on the Wicket form, but not in the metadata: `required` of `@PropertyInfo` doesn't reach a
  // reference (see ToDoEntityRest.validate, which checks it, too).
  assignee: m
    .entityField("assignee")
    .refine((v): boolean => v != null, REQUIRED),
  reporter: m.entityField("reporter"),
  task: m.entityField("task"),
  group: m.entityField("group"),
  resubmission: m.instantField("resubmission"),
  description: m.nullableString("description"),
  comment: m.nullableString("comment"),
  /** No metadata: a choice about the save, not a property of the to-do (see SendNotificationOption). */
  sendNotification: z.boolean(),
});

export type ToDoValues = z.infer<typeof toDoSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const TO_DO_FIELDS = Object.keys(
  toDoSchema.shape
) as readonly (keyof ToDoValues)[];
