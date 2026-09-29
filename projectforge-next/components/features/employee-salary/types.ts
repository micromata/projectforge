// Mirrors org.projectforge.rest.dto.EmployeeSalary (projectforge-rest). Keep field names in sync with
// the Spring DTO — the employee is a reference ({id, displayName}), and the read-only lastName /
// firstName / staffNumber the list shows are filled from it by EmployeeSalary.copyFrom, not editable.

import type { EMPLOYEE_SALARY_METADATA } from "@/lib/metadata/employee-salary.generated";

/** The constants of org.projectforge.business.fibu.EmployeeSalaryType, from the metadata. */
export type EmployeeSalaryType =
  (typeof EMPLOYEE_SALARY_METADATA.fields.type.enumValues)[number]["value"];

/** The referenced employee as the DTO carries it (org.projectforge.rest.dto.Employee, minimal). */
export interface EmployeeSalaryEmployee {
  id: number;
  displayName?: string | null;
}

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface EmployeeSalaryDetail {
  /** null for a salary that has not been saved yet (Spring assigns the id). */
  id: number | null;
  employee?: EmployeeSalaryEmployee | null;
  year?: number | null;
  month?: number | null;
  type?: EmployeeSalaryType | null;
  bruttoMitAgAnteil?: number | null;
  comment?: string | null;
  /**
   * `year-MM`, computed by the entity (EmployeeSalaryDO.formattedYearAndMonth has no backing field).
   * Read-only: the list shows it, the form never sends one back.
   */
  formattedYearAndMonth?: string | null;
  /** Read-only, filled from the employee's user — the columns Wicket sorts the list by. */
  lastName?: string | null;
  firstName?: string | null;
  /** Read-only, from the employee. */
  staffNumber?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface EmployeeSalaryListRow extends EmployeeSalaryDetail {
  id: number;
}
