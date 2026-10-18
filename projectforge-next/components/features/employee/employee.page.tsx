import { EMPLOYEE_METADATA } from "@/lib/metadata/employee.generated";
import { definePage } from "@/lib/page-def/define-page";
import { EmployeeEditIntro } from "./edit/employee-edit-intro";
import { EmployeeKontoField } from "./edit/employee-konto-field";
import {
  EmployeeValidSinceSection,
  employeeValidSinceTitleKey,
} from "./edit/employee-valid-since-section";
import { EmployeeListActions } from "./employee-list-actions";
import { EMPLOYEE_LIST_QUERY_KEY } from "./employee-query-key";
import {
  employeeSchema,
  EMPLOYEE_FIELDS,
  type EmployeeValues,
} from "./employee-schema";
import { emptyEmployeeValues, toFormValues } from "./employee-values";
import type {
  EmployeeDetail,
  EmployeeListRow,
  EmployeeValidSinceAttrType,
} from "./types";

/** A section of one time-dependent attribute; only for a stored employee, the entries need its id. */
function validSinceSection(id: string, type: EmployeeValidSinceAttrType) {
  return {
    id,
    titleKey: employeeValidSinceTitleKey(type),
    visible: ({ data }: { data: Record<string, unknown> | undefined }) =>
      data?.id != null,
    render: ({ id: employeeId }: { id: number | null }) =>
      employeeId == null ? null : (
        <EmployeeValidSinceSection id={employeeId} type={type} />
      ),
  };
}

/**
 * The employees (HR): who of the users is employed, with cost unit, creditor account, team and dates, and
 * the attributes that change over time — status, weekly working hours and annual leave days, each valid
 * since a date. Migrated from the React UILayout page (`EmployeePagesRest`, gone).
 */
export const EMPLOYEE_PAGE = definePage<
  EmployeeListRow,
  EmployeeValues,
  EmployeeDetail,
  typeof EMPLOYEE_METADATA
>({
  entity: "employee",
  metadata: EMPLOYEE_METADATA,
  route: "/employee",
  queryKey: EMPLOYEE_LIST_QUERY_KEY,
  categoryKey: "menu.fibu._",
  titleKey: "fibu.employee.title.list",
  defaultSort: { id: "user.lastname" },
  // The columns of the legacy list, in its order.
  columns: [
    {
      id: "user.firstname",
      labelKey: "firstName",
      accessor: (row) => row.user?.firstname ?? "",
      referenceKey: "user",
      size: 160,
    },
    {
      id: "user.lastname",
      labelKey: "name",
      accessor: (row) => row.user?.lastname ?? "",
      referenceKey: "user",
      className: "font-semibold",
      size: 160,
    },
    { name: "status", size: 170 },
    { name: "staffNumber", size: 120 },
    { name: "kost1", size: 220 },
    {
      id: "konto",
      labelKey: "fibu.employee.konto._",
      accessor: (row) => row.konto?.displayName ?? "",
      size: 220,
    },
    { name: "position", size: 160 },
    { name: "abteilung", size: 140 },
    { name: "eintrittsDatum", size: 120 },
    { name: "austrittsDatum", size: 120 },
    { name: "weeklyWorkingHours", size: 120 },
    { name: "annualLeave", size: 120, hiddenByDefault: true },
    { name: "comment", size: 300 },
    { name: "created", size: 130, hiddenByDefault: true },
    { name: "lastUpdate", size: 130, hiddenByDefault: true },
  ],
  listActions: EmployeeListActions,
  edit: {
    schema: employeeSchema,
    fieldNames: EMPLOYEE_FIELDS,
    defaultValues: emptyEmployeeValues,
    toFormValues,
    title: (employee) =>
      employee.displayName ?? employee.user?.displayName ?? "",
    newTitleKey: "fibu.employee.title.add",
    savedMessageKey: "message.successfullChanged",
    editIntro: EmployeeEditIntro,
    sections: [
      {
        id: "general",
        titleKey: "fibu.employee.title.heading",
        fields: [
          { name: "user", span: 2 },
          { name: "staffNumber" },
          { name: "kost1" },
          { custom: EmployeeKontoField, span: 2 },
          { name: "abteilung" },
          { name: "position" },
          { name: "eintrittsDatum" },
          { name: "austrittsDatum" },
          { name: "comment", span: 3, rows: 4 },
        ],
      },
      validSinceSection("status", "STATUS"),
      validSinceSection("weeklyWorkingHours", "WEEKLY_HOURS"),
      validSinceSection("annualLeave", "ANNUAL_LEAVE"),
    ],
  },
});
