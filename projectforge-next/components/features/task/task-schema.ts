import { z } from "zod";
import { TASK_METADATA } from "@/lib/metadata/task.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Every rule below — mandatory, maximum length, the bounds of the numbers, the constants of the four
 * enums — comes from `TaskDO` through `lib/metadata/task.generated.ts`. Nothing is restated here: the
 * ranges of `progress` (0-100), `maxHours` (0-9999) and `duration` (0-10000) are declared on the
 * entity's properties (`@PropertyInfo(min = …, max = …)`) and enforced by the backend itself
 * (`ValidationUtils.validateFields`), so the form only spares the user a round trip.
 */
const m = fromMetadata(TASK_METADATA);

/**
 * Which fields the form has mirrors org.projectforge.rest.dto.Task — a hand-written decision, because
 * the DTO has neither the field set nor the types of the DO (its dates are `LocalDate`, its references
 * are nested DTOs). What each field *allows* is not.
 *
 * The dates are strings: a `LocalDate` travels as `yyyy-MM-dd` and is held as that text, the same as
 * every other date of a next form (see `nullableString` and `InputField type="date"`).
 */
export const taskSchema = z.object({
  // null while the task is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  parentTask: m.entityField("parentTask"),
  title: m.requiredString("title"),
  status: m.enumField("status"),
  priority: m.enumField("priority"),
  shortDescription: m.nullableString("shortDescription"),
  description: m.nullableString("description"),
  progress: m.intField("progress"),
  maxHours: m.intField("maxHours"),
  startDate: m.nullableString("startDate"),
  endDate: m.nullableString("endDate"),
  duration: m.decimalField("duration"),
  protectTimesheetsUntil: m.nullableString("protectTimesheetsUntil"),
  responsibleUser: m.entityField("responsibleUser"),
  reference: m.nullableString("reference"),
  timesheetBookingStatus: m.enumField("timesheetBookingStatus"),
  kost2BlackWhiteList: m.nullableString("kost2BlackWhiteList"),
  kost2IsBlackList: m.booleanField("kost2IsBlackList"),
  protectionOfPrivacy: m.booleanField("protectionOfPrivacy"),
  allowTimeOverlap: m.booleanField("allowTimeOverlap"),
  // No box of its own anywhere (see types.ts) — carried so a save doesn't erase what is stored.
  workpackageCode: m.nullableString("workpackageCode"),
  ganttPredecessorOffset: m.intField("ganttPredecessorOffset"),
  ganttRelationType: m.enumField("ganttRelationType"),
  ganttObjectType: m.enumField("ganttObjectType"),
  ganttPredecessor: m.entityField("ganttPredecessor"),
  created: m.nullableString("created"),
});

export type TaskValues = z.infer<typeof taskSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const TASK_FIELDS = Object.keys(
  taskSchema.shape
) as readonly (keyof TaskValues)[];
