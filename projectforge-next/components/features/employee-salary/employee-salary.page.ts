import { EMPLOYEE_SALARY_METADATA } from "@/lib/metadata/employee-salary.generated";
import { definePage } from "@/lib/page-def/define-page";
import { EmployeeSalaryListActions } from "./employee-salary-list-actions";
import {
  employeeSalarySchema,
  EMPLOYEE_SALARY_FIELDS,
  type EmployeeSalaryValues,
} from "./employee-salary-schema";
import {
  emptyEmployeeSalaryValues,
  toFormValues,
} from "./employee-salary-values";
import type { EmployeeSalaryDetail, EmployeeSalaryListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const EMPLOYEE_SALARY_LIST_QUERY_KEY = ["employeeSalary"] as const;

/**
 * The whole employee-salary page — list and edit — as data (see lib/page-def/types.ts).
 *
 * The columns are those the legacy Wicket `EmployeeSalaryListPage` shows, in its order; the scalar
 * labels, the type texts and every rule come from EmployeeSalaryDO through the generated metadata. The
 * month/year the list is read by is one computed column (`formattedYearAndMonth`, the entity's own
 * transient `year-MM`), sorted by the columns it is made of (EmployeeSalaryEntityRest.postProcessMagicFilter);
 * the employee's name and staff number are nested paths the metadata does not cover, so they carry
 * their own label and sort path.
 */
export const EMPLOYEE_SALARY_PAGE = definePage<
  EmployeeSalaryListRow,
  EmployeeSalaryValues,
  EmployeeSalaryDetail,
  typeof EMPLOYEE_SALARY_METADATA
>({
  entity: "employeeSalary",
  metadata: EMPLOYEE_SALARY_METADATA,
  route: "/employeeSalary",
  queryKey: EMPLOYEE_SALARY_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Finance > Salaries (MenuItemDefId.EMPLOYEE_SALARY_LIST).
  categoryKey: "menu.fibu._",
  titleKey: "fibu.employee.salary.title.list",
  // Newest first, as Wicket's list (EmployeeSalaryDao orders year, month descending). No column holds
  // the value, so the backend maps this id onto year+month (EmployeeSalaryEntityRest.postProcessMagicFilter).
  defaultSort: { id: "formattedYearAndMonth", desc: true },
  columns: [
    // Read as one value ("2026-09"), not two — the transient formattedYearAndMonth. Sorted by the
    // backend, which maps it onto year+month; no column of its own holds it.
    {
      id: "formattedYearAndMonth",
      labelKey: "calendar.month",
      accessor: (row) => row.formattedYearAndMonth ?? "",
      className: "font-mono font-semibold",
      size: 110,
    },
    // The employee, split as Wicket splits it — sorted by the entity's own paths (employee.user.*,
    // employee.staffNumber), read from the reference this list already resolved (see transformFromDB).
    {
      id: "employee.user.lastname",
      labelKey: "name",
      accessor: (row) => row.lastName ?? "",
      referenceKey: "employee",
      size: 160,
    },
    {
      id: "employee.user.firstname",
      labelKey: "firstName",
      accessor: (row) => row.firstName ?? "",
      referenceKey: "employee",
      size: 160,
    },
    {
      id: "employee.staffNumber",
      labelKey: "fibu.employee.staffNumber",
      accessor: (row) => row.staffNumber ?? "",
      referenceKey: "employee",
      size: 120,
    },
    { name: "type", size: 140 },
    { name: "bruttoMitAgAnteil", size: 160 },
    { name: "comment", size: 300 },
    // When an entry was created and last changed — the two the legacy list omits.
    { name: "created", size: 130, hiddenByDefault: true },
    { name: "lastUpdate", size: 130, hiddenByDefault: true },
  ],
  listActions: EmployeeSalaryListActions,
  // Payment type and comment of several salaries at once (EmployeeSalaryMultiSelectedPageRest).
  massUpdate: {
    endpoint: "employeeSalarySelected",
    route: "/employeeSalary/mass-update",
  },
  edit: {
    schema: employeeSalarySchema,
    fieldNames: EMPLOYEE_SALARY_FIELDS,
    defaultValues: emptyEmployeeSalaryValues,
    toFormValues,
    // Who and which month the salary is for — the same the list shows.
    title: (salary) =>
      [salary.employee?.displayName, salary.formattedYearAndMonth]
        .filter(Boolean)
        .join(" "),
    newTitleKey: "fibu.employee.salary.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "general",
        titleKey: "fibu.employee.salary.title.heading",
        fields: [
          // First, the one value the user always has to pick (EmployeeSalaryEditForm focuses it).
          { name: "employee", span: 3 },
          // Month and year on one line, as Wicket's combined "Month / Year" fieldset.
          {
            group: [
              { name: "month", maxDigits: 2 },
              { name: "year", maxDigits: 4 },
            ],
          },
          { name: "type", emphasized: true },
          { name: "bruttoMitAgAnteil" },
          { name: "comment", span: 3, rows: 4 },
        ],
      },
    ],
  },
});
