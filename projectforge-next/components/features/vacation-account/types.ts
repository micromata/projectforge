/**
 * The vacation account of an employee, as `GET /rs/vacationAccount` answers it (mirrors
 * `org.projectforge.rest.dto.VacationAccount`). Every value comes formatted for the logged-in user; the urls
 * are paths of the app (`/next/…`, `/react/…`), resolved with resolveMenuUrl.
 */
export interface VacationAccount {
  /** Null if the logged-in user has no employee: then everything else is empty. */
  employee?: { id: number; displayName?: string } | null;
  /** HR staff: may choose any employee and recalculate the remaining leave. */
  hrAccess: boolean;
  canRecalculate: boolean;
  /** Previous, current and next year. */
  statistics: VacationStatsFormatted[];
  /** The label of the unused carry, naming the day it expires on. */
  previousYearLeaveUnusedLabel?: string;
  /** Next, current and previous year. */
  years: VacationAccountYear[];
  /** Newest first. */
  leaveAccountEntries: VacationAccountLeaveEntry[];
  workingHoursStatistics?: string;
  newVacationUrl?: string;
  /** Of the employee's user, overlapping the three years, newest first: the other absences. */
  availabilities: VacationAccountAvailability[];
  /** A new availability entry of the employee's user; absent for an employee without a user. */
  newAvailabilityUrl?: string;
  vacationListUrl: string;
  exportUrl: string;
}

/** `org.projectforge.business.vacation.service.VacationStatsFormatted`, the fields shown. */
export interface VacationStatsFormatted {
  year: number;
  vacationDaysInYearFromContract?: string;
  remainingLeaveFromPreviousYear?: string;
  totalLeaveIncludingCarry?: string;
  remainingLeaveFromPreviousYearUnused?: string;
  leaveAccountEntriesSum?: string;
  vacationDaysApproved?: string;
  vacationDaysInProgress?: string;
  vacationDaysLeftInYear?: string;
  specialVacationDaysApproved?: string;
  specialVacationDaysInProgress?: string;
}

export interface VacationAccountYear {
  year: number;
  vacations: VacationAccountVacation[];
}

export interface VacationAccountVacation {
  id: number;
  startDateFormatted?: string;
  endDateFormatted?: string;
  statusString?: string;
  workingDaysFormatted?: string;
  specialFormatted?: string;
  replacement?: string;
  otherReplacements?: string;
  manager?: string;
  vacationModeString?: string;
  comment?: string;
  /** A day of it without any replacement on duty. */
  conflict: boolean;
  /** For the days bar (AbsenceDaysBar), ISO dates. */
  startDate?: string | null;
  endDate?: string | null;
  halfDayBegin?: boolean | null;
  halfDayEnd?: boolean | null;
  /** Weekends and holidays; null for a period the bar doesn't draw. */
  nonWorkingDays?: string[] | null;
  /** The days without any replacement on duty, only on a conflict. */
  uncoveredDays?: string[] | null;
  editUrl: string;
}

export interface VacationAccountLeaveEntry {
  id: number;
  dateFormatted?: string;
  amountFormatted?: string;
  description?: string;
  editUrl: string;
}

/** `org.projectforge.rest.dto.VacationAccountAvailability`. */
export interface VacationAccountAvailability {
  id: number;
  startDateFormatted?: string;
  endDateFormatted?: string;
  type?: string;
  statusString?: string;
  locationString?: string;
  comment?: string;
  /** The names, comma separated. */
  substitutes?: string;
  /** A day from today on without any substitute on duty. */
  conflict: boolean;
  /** Its working days (weekends and holidays not counted). */
  workingDaysFormatted?: string;
  /** For the days bar (AbsenceDaysBar), ISO dates. */
  startDate?: string | null;
  endDate?: string | null;
  /** Weekends and holidays; null for a period the bar doesn't draw. */
  nonWorkingDays?: string[] | null;
  /** The days without any substitute on duty, only on a conflict. */
  uncoveredDays?: string[] | null;
  editUrl: string;
}
