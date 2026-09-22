import { z } from "zod";
import { TIMESHEET_METADATA } from "@/lib/metadata/timesheet.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { i18nMarker } from "@/lib/validation/markers";

/**
 * Every rule below — mandatory, maximum length, the constants of the AI unit — comes from TimesheetDO
 * through `lib/metadata/timesheet.generated.ts`. Nothing here restates one (see
 * lib/validation/from-metadata.ts).
 */
const m = fromMetadata(TIMESHEET_METADATA);

/**
 * Which fields the form has mirrors org.projectforge.rest.dto.Timesheet — hand-written, because the DTO
 * has neither the field set nor the names of the DO. What each field *allows* is not.
 *
 * The server validates too and has the last word (`TimesheetDao.onInsertOrModify` and
 * `TimesheetPagesRest.validate`, HTTP 406 → see lib/validation/server-errors.ts). The rules it owns are
 * deliberately absent here, because none of them is the client's to know: whether the task is bookable
 * at all, whether a cost unit is required for it, whether the period collides with another sheet of the
 * same user, whether it violates the task's time sheet protection, and the maximum duration. What is
 * anticipated is only what the user can see on the form itself.
 */
const timesheetEditObject = z.object({
  // null while the sheet is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  // Optional per TaskDO's annotation, and the server refuses a sheet without one
  // (`timesheet.error.invalidTaskId`): the metadata is what decides, so the form does not add a rule
  // the entity doesn't declare.
  task: m.entityField("task"),
  user: m.entityField("user"),
  // Required only for a task that *has* cost units, which is the task's property and not the field's —
  // hence optional here and `timesheet.error.kost2Required` from the server (see TaskKost2Section).
  kost2: m.entityField("kost2"),
  startTime: m.instantField("startTime"),
  stopTime: m.instantField("stopTime"),
  location: m.nullableString("location"),
  reference: m.nullableString("reference"),
  tag: m.nullableString("tag"),
  description: m.nullableString("description"),
  timeSavedByAI: m.decimalField("timeSavedByAI"),
  timeSavedByAIUnit: m.enumField("timeSavedByAIUnit"),
  timeSavedByAIDescription: m.nullableString("timeSavedByAIDescription"),
  created: m.nullableString("created"),
});

export const timesheetEditSchema = timesheetEditObject
  // The one cross-field rule worth anticipating, because both boxes are on the screen together and the
  // answer needs nothing the client doesn't have. Reported on the stop time, which is the end the user
  // fixes — the same key the legacy period panel used (`timePeriodPanel.startTimeAfterStopTime`).
  .refine((v) => !v.startTime || !v.stopTime || v.startTime < v.stopTime, {
    path: ["stopTime"],
    message: i18nMarker("timePeriodPanel.startTimeAfterStopTime"),
  });

export type TimesheetEditValues = z.infer<typeof timesheetEditSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 *
 * Read off the pre-refine object: `timesheetEditSchema` is a `ZodPipe` after the `refine` above and has
 * no `shape` of its own.
 */
export const TIMESHEET_EDIT_FIELDS = Object.keys(
  timesheetEditObject.shape
) as readonly (keyof TimesheetEditValues)[];
