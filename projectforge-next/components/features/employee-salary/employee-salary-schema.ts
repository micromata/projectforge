import { z } from "zod";
import { EMPLOYEE_SALARY_METADATA } from "@/lib/metadata/employee-salary.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import {
  INTEGER,
  REQUIRED,
  maxMarker,
  minMarker,
} from "@/lib/validation/markers";

/**
 * The rules that EmployeeSalaryDO carries — the maximum length of the comment, the constants of the
 * type enum — come from `lib/metadata/employee-salary.generated.ts` through [fromMetadata]. What this
 * file adds by hand are the three fields the entity marks optional but Wicket requires: the employee,
 * the year and the month (see EmployeeSalaryEditForm, all `setRequired(true)`; the DO column
 * `employee_id` is `nullable = false` and month/year back the unique constraint). The bounds of year
 * and month are Wicket's own (MinMaxNumberField 1900-2999, the twelve months).
 */
const m = fromMetadata(EMPLOYEE_SALARY_METADATA);

/** The employee's own bounds mirror Wicket's month/year inputs (EmployeeSalaryEditForm). */
const MONTH_MIN = 1;
const MONTH_MAX = 12;
const YEAR_MIN = 1900;
const YEAR_MAX = 2999;

/** The DTO carries the employee reference as `{id, displayName}`; only the id is written back. */
const employeeRef = z
  .looseObject({ id: z.number(), displayName: z.string().optional() })
  .nullable()
  .refine((v): boolean => v != null, REQUIRED);

/**
 * A whole number that is mandatory and bounded, for a field the metadata marks optional (year/month
 * have no `required` in `@PropertyInfo`, but Wicket makes them mandatory). Reproduces
 * [fromMetadata.intField] with an added `required` refine and hand-given bounds, the same way cost2's
 * number segments do. The markers are the backend's, so the wording matches the HTTP 406.
 */
function requiredInt(min: number, max: number) {
  return z
    .number()
    .nullable()
    .refine((v) => v == null || Number.isInteger(v), INTEGER)
    .refine((v): boolean => v != null, REQUIRED)
    .refine((v) => v == null || v >= min, minMarker(min))
    .refine((v) => v == null || v <= max, maxMarker(max));
}

/**
 * Which fields the form has mirrors org.projectforge.rest.dto.EmployeeSalary — a hand-written
 * decision. What each field allows is not (see above). `formattedYearAndMonth` and the read-only
 * lastName / firstName / staffNumber the list shows are deliberately absent from what is *edited*: the
 * employee, year and month are the inputs, and EmployeeSalary.copyFrom derives the rest.
 */
export const employeeSalarySchema = z.object({
  // null while the salary is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  employee: employeeRef,
  year: requiredInt(YEAR_MIN, YEAR_MAX),
  month: requiredInt(MONTH_MIN, MONTH_MAX),
  type: m.enumField("type"),
  bruttoMitAgAnteil: m.decimalField("bruttoMitAgAnteil"),
  comment: m.nullableString("comment"),
});

export type EmployeeSalaryValues = z.infer<typeof employeeSalarySchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const EMPLOYEE_SALARY_FIELDS = Object.keys(
  employeeSalarySchema.shape
) as readonly (keyof EmployeeSalaryValues)[];
