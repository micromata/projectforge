import type {
  HRPlanningEntryValues,
  HRPlanningValues,
} from "./hr-planning-schema";
import type { HRPlanningDetail, HRPlanningEntryDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value.
 */
export function toEntryValues(
  entry: HRPlanningEntryDetail
): HRPlanningEntryValues {
  return {
    id: entry.id ?? null,
    deleted: entry.deleted ?? false,
    status: (entry.status ?? null) as HRPlanningEntryValues["status"],
    projekt: entry.projekt ?? null,
    priority: (entry.priority ?? null) as HRPlanningEntryValues["priority"],
    probability: entry.probability ?? null,
    unassignedHours: entry.unassignedHours ?? null,
    mondayHours: entry.mondayHours ?? null,
    tuesdayHours: entry.tuesdayHours ?? null,
    wednesdayHours: entry.wednesdayHours ?? null,
    thursdayHours: entry.thursdayHours ?? null,
    fridayHours: entry.fridayHours ?? null,
    weekendHours: entry.weekendHours ?? null,
    description: entry.description ?? null,
  };
}

export function toFormValues(planning: HRPlanningDetail): HRPlanningValues {
  return {
    id: planning.id ?? null,
    user: planning.user ?? null,
    week: planning.week ?? "",
    entries: (planning.entries ?? []).map(toEntryValues),
    created: planning.created ?? null,
  };
}

/** A blank entry, as the "add" button and an empty new week start with. */
export function emptyEntryValues(): HRPlanningEntryValues {
  return toEntryValues({});
}

/**
 * Blank form for a week that isn't planned yet. User, week and the first blank entry are preset by the
 * backend (`HRPlanningEntityRest.newBaseDTO`), whose `/rs/hrPlanning/newEntry` answer the form is reset
 * onto.
 */
export function emptyHRPlanningValues(): HRPlanningValues {
  return toFormValues({ id: null });
}

/** The hours of an entry summed, as `HRPlanningEntryDO.totalHours` does. */
export function entryTotalHours(entry: HRPlanningEntryValues): number {
  return [
    entry.unassignedHours,
    entry.mondayHours,
    entry.tuesdayHours,
    entry.wednesdayHours,
    entry.thursdayHours,
    entry.fridayHours,
    entry.weekendHours,
  ].reduce<number>((sum, hours) => sum + (hours ?? 0), 0);
}

/**
 * Whether an entry holds nothing yet — what "copy from predecessor" replaces, as the legacy form did
 * (`HRPlanningEditForm`, removing the empty entries before adding the copies).
 */
export function isEmptyEntry(entry: HRPlanningEntryValues): boolean {
  return (
    entry.id == null &&
    entry.status == null &&
    entry.projekt == null &&
    !entry.description &&
    entryTotalHours(entry) === 0
  );
}
