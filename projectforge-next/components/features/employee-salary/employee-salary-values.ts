import type { EmployeeSalaryValues } from "./employee-salary-schema";
import type { EmployeeSalaryDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` — which a controlled
 * input would read as "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(
  salary: EmployeeSalaryDetail
): EmployeeSalaryValues {
  return {
    id: salary.id ?? null,
    // The reference the picker binds to: {id, displayName}, all the server needs to resolve it by id.
    employee: salary.employee
      ? {
          id: salary.employee.id,
          displayName: salary.employee.displayName ?? "",
        }
      : null,
    year: salary.year ?? null,
    month: salary.month ?? null,
    type: salary.type ?? null,
    bruttoMitAgAnteil: salary.bruttoMitAgAnteil ?? null,
    comment: salary.comment ?? null,
  };
}

/**
 * Blank form for a salary that doesn't exist yet.
 *
 * The current year and month are proposed (Wicket did the same through its per-user recent entry, the
 * common case being "this month's salary"); the type starts at GEHALT, the enum's first and by far most
 * frequent value and Wicket's default. The employee is the one value the user always has to pick.
 */
export function emptyEmployeeSalaryValues(): EmployeeSalaryValues {
  const now = new Date();
  return {
    id: null,
    employee: null,
    year: now.getFullYear(),
    month: now.getMonth() + 1,
    type: "GEHALT",
    bruttoMitAgAnteil: null,
    comment: null,
  };
}
