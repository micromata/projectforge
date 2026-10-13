// Mirrors org.projectforge.rest.dto.Vacation. Keep field names in sync with the Spring DTO — the formatted
// values, `conflict`, `conflictingVacations`, `allowedStatus` and `employeeEditable` have no counterpart in
// the generated metadata.

import type { VACATION_METADATA } from "@/lib/metadata/vacation.generated";

/** The constants of org.projectforge.business.vacation.model.VacationStatus, from the metadata. */
export type VacationStatus =
  (typeof VACATION_METADATA.fields.status.enumValues)[number]["value"];

/**
 * A referenced employee as the DTO carries it: the id to write back, the name to show. A type alias rather
 * than an interface, so it satisfies the index signature of the schema's `looseObject`.
 */
export type EmployeeRef = {
  id: number;
  displayName?: string;
};

/** A day of the chart of the overlapping vacations (`org.projectforge.rest.dto.VacationChartDay`). */
export interface VacationChartDay {
  /** ISO date (yyyy-MM-dd). */
  date: string;
  /** Neither weekend nor holiday. */
  workingDay: boolean;
  holiday?: string | null;
  /** A day of the vacation on which no substitute is on duty. */
  uncovered?: boolean;
}

/**
 * What the form shows beside its fields and the backend computes for the state being edited
 * (`VacationEntityRest.VacationFormStats`, `/rs/vacation/stats`). The loaded entry carries the same
 * values as stored.
 */
export interface VacationFormStats {
  workingDays?: number | null;
  workingDaysFormatted?: string | null;
  vacationDaysLeftInYear?: number | null;
  vacationDaysLeftInYearString?: string | null;
  /** The days left once this entry is counted as edited instead of as stored (a new one isn't at all). */
  vacationDaysLeftAfterThis?: number | null;
  vacationDaysLeftAfterThisString?: string | null;
  /** The vacations of the replacements overlapping this one. */
  conflictingVacations?: VacationDetail[] | null;
  /** At least one day of the period without any replacement on duty. */
  conflict?: boolean | null;
  /** The statuses the logged-in user may set (`VacationDao.getAllowedStatus`). */
  allowedStatus?: VacationStatus[] | null;
  /** The axis of the chart: the period with a margin of three days on either side. */
  chartDays?: VacationChartDay[] | null;
}

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface VacationDetail extends VacationFormStats {
  /** null for an entry that has not been saved yet (Spring assigns the id). */
  id: number | null;
  employee?: EmployeeRef | null;
  startDate?: string | null;
  endDate?: string | null;
  /** The dates in the user's format, for the vacations of the replacements (not edited there). */
  startDateFormatted?: string | null;
  endDateFormatted?: string | null;
  halfDayBegin?: boolean | null;
  halfDayEnd?: boolean | null;
  manager?: EmployeeRef | null;
  replacement?: EmployeeRef | null;
  otherReplacements?: EmployeeRef[] | null;
  status?: VacationStatus | null;
  /** Translated status, for the list. */
  statusString?: string | null;
  /** Own, replacement, manager or other — relative to the logged-in user, translated. */
  vacationModeString?: string | null;
  special?: boolean | null;
  comment?: string | null;
  /** Whether the logged-in user may choose the employee (HR staff only). Read-only. */
  employeeEditable?: boolean;
  deleted?: boolean;
  writeAccess?: boolean;
  deleteAccess?: boolean;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface VacationListRow extends VacationDetail {
  id: number;
  /** The weekends and holidays of the period (ISO dates), for the bar of the list. */
  nonWorkingDays?: string[] | null;
  /** The days without any substitute on duty (ISO dates), only on a conflict. */
  uncoveredDays?: string[] | null;
}
