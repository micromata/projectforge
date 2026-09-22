import type { TimesheetEditValues } from "./timesheet-edit-schema";
import type { Kost2Ref, TimesheetDetail } from "../types";

/**
 * A cost unit as the form holds it: `{id, displayName}` like every other reference, with the number the
 * backend formatted as the name — that is what a cost unit is called ("5.100.01.02"), and the DTO
 * carries no `displayName` of its own for it (`Kost2.formattedNumber`, see Kost2Ref).
 */
export function toKost2Ref(
  kost2: Kost2Ref | null | undefined
): Kost2Ref | null {
  if (!kost2) return null;
  return {
    ...kost2,
    displayName: kost2.displayName || kost2.formattedNumber || String(kost2.id),
  };
}

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised to null here, so no field ever holds `undefined` — which a
 * controlled input would read as "uncontrolled" and the schema as a missing value.
 *
 * `startTime`/`stopTime` are handed on as they came: the wire format is already the ISO instant in UTC
 * that DateTimeInput consumes, and converting it into the user's zone is that input's business, not a
 * form value's (see lib/user-zone.ts).
 */
export function toFormValues(timesheet: TimesheetDetail): TimesheetEditValues {
  return {
    id: timesheet.id ?? null,
    task: timesheet.task ?? null,
    user: timesheet.user ?? null,
    kost2: toKost2Ref(timesheet.kost2),
    startTime: timesheet.startTime ?? null,
    stopTime: timesheet.stopTime ?? null,
    location: timesheet.location ?? null,
    reference: timesheet.reference ?? null,
    tag: timesheet.tag ?? null,
    description: timesheet.description ?? null,
    timeSavedByAI: timesheet.timeSavedByAI ?? null,
    // What the entity's default is (`TimesheetDO.timeSavedByAIUnit`), and what `newEntry` answers with;
    // only a sheet stored before the field existed has none.
    timeSavedByAIUnit: timesheet.timeSavedByAIUnit ?? "PERCENTAGE",
    timeSavedByAIDescription: timesheet.timeSavedByAIDescription ?? null,
    created: timesheet.created ?? null,
  };
}

/**
 * The values a template (a recent entry or a saved favorite) carries — the *what* of a sheet, never its
 * *when*: a template is a way of booking the same work again, not at the same time (see
 * `TimesheetRecentEntry`, which has no period). Applied over the form, they leave `startTime`/`stopTime`
 * as the user set them.
 *
 * Not the whole DTO: `id`, `user` and `created` belong to the sheet being edited, not to the template it
 * is filled from, and copying them would turn an edit into a save over the wrong row.
 */
export function templateFieldsOf(
  timesheet: TimesheetDetail
): Partial<TimesheetEditValues> {
  return {
    task: timesheet.task ?? null,
    kost2: toKost2Ref(timesheet.kost2),
    location: timesheet.location ?? null,
    reference: timesheet.reference ?? null,
    tag: timesheet.tag ?? null,
    description: timesheet.description ?? null,
    timeSavedByAI: timesheet.timeSavedByAI ?? null,
    timeSavedByAIUnit: timesheet.timeSavedByAIUnit ?? "PERCENTAGE",
    timeSavedByAIDescription: timesheet.timeSavedByAIDescription ?? null,
  };
}

/**
 * The form as a DTO the backend reads (`org.projectforge.rest.dto.Timesheet`): the values are already
 * that shape, so this is a widening for the calls that post the current sheet without saving it — a
 * template select merges its entry *into* the posted sheet (see selectRecentTimesheet).
 */
export function toTimesheetDetail(
  values: TimesheetEditValues
): TimesheetDetail {
  return values;
}

/**
 * Blank form for a sheet that doesn't exist yet, i.e. what is on screen for the moment before the
 * preset arrives.
 *
 * Deliberately empty of the values that matter: the user, the two ends of the period and the task all
 * come from `timesheet/newEntry` — the backend takes the user from the session, presets the period from
 * the calendar's parameters and prefills task, cost unit and texts from the user's most recent sheet
 * (`TimesheetPagesRest.newBaseDTO`). Guessing any of them here would mean a form that briefly shows
 * something else than what is being edited.
 */
export function emptyTimesheetValues(): TimesheetEditValues {
  return {
    id: null,
    task: null,
    user: null,
    kost2: null,
    startTime: null,
    stopTime: null,
    location: null,
    reference: null,
    tag: null,
    description: null,
    timeSavedByAI: null,
    timeSavedByAIUnit: "PERCENTAGE",
    timeSavedByAIDescription: null,
    created: null,
  };
}
