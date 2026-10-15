// Mirrors org.projectforge.rest.dto.HRPlanning and org.projectforge.rest.dto.HRPlanningEntry. Keep field
// names in sync with the Spring DTOs — `projekt` and `status` have no counterpart in the generated
// metadata of the entry's project (ProjektDO has no UIDataType), the list-row fields none at all.

/**
 * A referenced user or project as the DTO carries it: the id to write back (`BaseDTO.copyTo` resolves
 * it), the name to show. A type alias rather than an interface, so it satisfies the index signature of
 * the schema's `looseObject`.
 */
export type EntityRefDto = {
  id: number;
  displayName?: string;
  deleted?: boolean;
};

/**
 * One entry of a planned week: either a project or a status (absence, illness, ...), and the hours per
 * day. Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL`, so an empty field is absent from the JSON.
 */
export interface HRPlanningEntryDetail {
  id?: number | null;
  deleted?: boolean;
  /** `HRPlanningEntryStatus` (ABSENT, ILL, LEAVE, OTHER); exclusive with [projekt]. */
  status?: string | null;
  projekt?: EntityRefDto | null;
  /** The project's name or the translated status, as the legacy list showed it. Read-only. */
  projektNameOrStatus?: string | null;
  /** `Priority` (LEAST ... HIGHEST). */
  priority?: string | null;
  probability?: number | null;
  /** The sum of the hours of this entry. Read-only. */
  totalHours?: number | null;
  unassignedHours?: number | null;
  mondayHours?: number | null;
  tuesdayHours?: number | null;
  wednesdayHours?: number | null;
  thursdayHours?: number | null;
  fridayHours?: number | null;
  weekendHours?: number | null;
  description?: string | null;
}

/** A planned week of one employee with its entries — the form (`/rs/hrPlanning`). */
export interface HRPlanningDetail {
  /** null for a planning that has not been saved yet (Spring assigns the id). */
  id: number | null;
  deleted?: boolean;
  user?: EntityRefDto | null;
  /** The Monday of the week, ISO date. */
  week?: string | null;
  /** The year and calendar week, e.g. "2026-40". Read-only. */
  formattedWeekOfYear?: string | null;
  /** The sum of the hours of all entries. Read-only. */
  totalHours?: number | null;
  entries?: HRPlanningEntryDetail[] | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/**
 * A row of the list (`/rs/hrPlanningEntry`): one entry with what it shows of its week
 * (`HRPlanningEntry.copyFrom4ListRow`). With "grouped" on, the rows are one per week and project
 * group, synthesized by `HRPlanningEntryDao.groupAndFilter`; their id is the one of the week.
 */
export interface HRPlanningListRow extends HRPlanningEntryDetail {
  id: number;
  /** The planned week a click opens. */
  planningId?: number | null;
  user?: EntityRefDto | null;
  week?: string | null;
  formattedWeekOfYear?: string | null;
  /** The customer of the project, by name. */
  kunde?: string | null;
  /** The sum of the hours of the whole week. */
  planningTotalHours?: number | null;
}

/** The sums of the list, `HRPlanningEntryEntityRest.HRPlanningListStatistics`. */
export interface HRPlanningListStatistics {
  totalHours?: number | null;
}
