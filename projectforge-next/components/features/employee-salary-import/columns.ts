import type { ImportColumn } from "@/components/shared/import/import-types";

/**
 * The preview columns of the employee-salary import: the staff number and resolved employee, the accounting
 * year/month read from the file's `Abrechnungsmonat` column, and the single imported value — the gross
 * amount including the employer's SV share, the only column the reconcile can change against an existing
 * salary (hence `diff`).
 */
export const EMPLOYEE_SALARY_IMPORT_COLUMNS: ImportColumn[] = [
  {
    field: "staffNumber",
    headerKey: "fibu.employee.staffNumber",
    kind: "integer",
    width: 110,
  },
  {
    field: "employee",
    headerKey: "name",
    kind: "text",
    width: 220,
  },
  {
    field: "year",
    headerKey: "calendar.year",
    kind: "integer",
    width: 80,
  },
  {
    field: "month",
    headerKey: "calendar.month",
    kind: "month",
    width: 80,
  },
  {
    field: "bruttoMitAgAnteil",
    headerKey: "fibu.employee.salary.bruttoMitAgAnteil",
    kind: "currency",
    diff: true,
    width: 160,
  },
];
