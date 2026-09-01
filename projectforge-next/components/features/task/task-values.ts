import type { TaskValues } from "./task-schema";
import type { TaskDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` — which a controlled
 * input would read as "uncontrolled" and the schema as a missing value.
 *
 * The three flags become `false` rather than null: they are Kotlin primitives on `TaskDO`
 * (`kost2IsBlackList`, `protectionOfPrivacy`, `allowTimeOverlap`), so "not set" is not a value the
 * backend can hold, and the DTO only reports them as nullable because it copies through a `Boolean?`.
 */
export function toFormValues(task: TaskDetail): TaskValues {
  return {
    id: task.id ?? null,
    parentTask: task.parentTask ?? null,
    // The one mandatory string of the form: never null, an emptied input holds "" (see requiredString).
    title: task.title ?? "",
    status: task.status ?? null,
    priority: task.priority ?? null,
    shortDescription: task.shortDescription ?? null,
    description: task.description ?? null,
    progress: task.progress ?? null,
    maxHours: task.maxHours ?? null,
    startDate: task.startDate ?? null,
    endDate: task.endDate ?? null,
    duration: task.duration ?? null,
    protectTimesheetsUntil: task.protectTimesheetsUntil ?? null,
    responsibleUser: task.responsibleUser ?? null,
    reference: task.reference ?? null,
    timesheetBookingStatus: task.timesheetBookingStatus ?? null,
    kost2BlackWhiteList: task.kost2BlackWhiteList ?? null,
    kost2IsBlackList: task.kost2IsBlackList ?? false,
    protectionOfPrivacy: task.protectionOfPrivacy ?? false,
    allowTimeOverlap: task.allowTimeOverlap ?? false,
    workpackageCode: task.workpackageCode ?? null,
    ganttPredecessorOffset: task.ganttPredecessorOffset ?? null,
    ganttRelationType: task.ganttRelationType ?? null,
    ganttObjectType: task.ganttObjectType ?? null,
    ganttPredecessor: task.ganttPredecessor ?? null,
    created: task.created ?? null,
  };
}

/**
 * Blank form for a task that doesn't exist yet — the same normalisation applied to a task with nothing
 * in it, rather than a second list of the same field names: a field added to [toFormValues] and
 * forgotten here would hold `undefined`, which is exactly the state that function exists to rule out.
 *
 * `timesheetBookingStatus` therefore starts unset although the metadata says mandatory, which is
 * intended: a value the user has to choose must be reportable as missing instead of pre-filled with
 * the first constant. Wicket's form arrives with `INHERIT`, but that is `TaskDO`'s field default and
 * comes with the new entry the backend answers (`/rs/task/newEntry`) — the form is filled from that as
 * soon as it arrives; this is what it shows in the meantime.
 */
export function emptyTaskValues(): TaskValues {
  return toFormValues({ id: null });
}
