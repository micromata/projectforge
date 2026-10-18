// Mirrors org.projectforge.rest.dto.Employee (projectforge-rest). Keep field names in sync with the Spring
// DTO — user, kost1 and konto are references ({id, displayName}); status, weeklyWorkingHours and
// annualLeave are read-only, the values in force today, computed from the valid-since entries.

import type { EMPLOYEE_METADATA } from "@/lib/metadata/employee.generated";
import type { ValidityEntry } from "@/components/shared/validity-entries/types";

export type EmployeeStatus =
  (typeof EMPLOYEE_METADATA.fields.status.enumValues)[number]["value"];

/** org.projectforge.business.fibu.EmployeeValidSinceAttrType */
export type EmployeeValidSinceAttrType =
  | "ANNUAL_LEAVE"
  | "STATUS"
  | "WEEKLY_HOURS";

/**
 * One time-dependent attribute of an employee (org.projectforge.rest.dto.EmployeeValidSinceAttr). The
 * value is a string on the wire for every type: an `EmployeeStatus`, a decimal or an integer; the dialog
 * holds the numbers as numbers (see lib/rs/employee-valid-since-attr).
 */
export interface EmployeeValidSinceAttr extends ValidityEntry {
  validSince?: string | null;
  value?: string | number | null;
  type?: EmployeeValidSinceAttrType | null;
  employeeId?: number | null;
}

export interface EmployeeRef {
  id: number;
  displayName?: string | null;
}

export interface EmployeeUserRef extends EmployeeRef {
  firstname?: string | null;
  lastname?: string | null;
  deactivated?: boolean | null;
}

export interface EmployeeDetail {
  /** null for an employee that has not been saved yet (Spring assigns the id). */
  id: number | null;
  displayName?: string | null;
  user?: EmployeeUserRef | null;
  kost1?: EmployeeRef | null;
  konto?: EmployeeRef | null;
  position?: string | null;
  abteilung?: string | null;
  staffNumber?: string | null;
  eintrittsDatum?: string | null;
  austrittsDatum?: string | null;
  comment?: string | null;
  /** Read-only, in force today. */
  status?: EmployeeStatus | null;
  /** Read-only, in force today. */
  weeklyWorkingHours?: number | null;
  /** Read-only, in force today. */
  annualLeave?: number | null;
  /** Only in the form (`transformFromDB` in edit mode), newest first. */
  statusEntries?: EmployeeValidSinceAttr[] | null;
  weeklyWorkingHoursEntries?: EmployeeValidSinceAttr[] | null;
  annualLeaveEntries?: EmployeeValidSinceAttr[] | null;
  writeAccess?: boolean | null;
  deleteAccess?: boolean | null;
  created?: string | null;
  lastUpdate?: string | null;
}

export interface EmployeeListRow extends EmployeeDetail {
  id: number;
}
