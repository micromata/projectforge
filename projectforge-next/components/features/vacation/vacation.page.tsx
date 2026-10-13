import { VACATION_METADATA } from "@/lib/metadata/vacation.generated";
import { definePage } from "@/lib/page-def/define-page";
import { VacationConflictNotice } from "./vacation-conflict-notice";
import {
  VacationEmployeeField,
  VacationOtherReplacementsField,
} from "./vacation-employee-fields";
import { VacationListActions } from "./vacation-list-actions";
import {
  vacationSchema,
  VACATION_FIELDS,
  type VacationValues,
} from "./vacation-schema";
import {
  VacationDaysLeftField,
  VacationWorkingDaysField,
} from "./vacation-stats-fields";
import { VacationStatusField } from "./vacation-status-field";
import { emptyVacationValues, toFormValues } from "./vacation-values";
import { VacationOverlapChart } from "./vacation-overlap-chart";
import { VacationsOfReplacements } from "./vacations-of-replacements";
import type { VacationDetail, VacationListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const VACATION_LIST_QUERY_KEY = ["vacation"] as const;

const displayName = (ref: { displayName?: string } | null | undefined) =>
  ref?.displayName ?? null;

/**
 * The vacation entries — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the React UILayout pages of `VacationPagesRest`, which it follows: the list's columns in their
 * order with the entries of a day without any replacement on duty in red, and the form's fields with the
 * working days, the days left and the vacations of the replacements beside them. What the form shows beside
 * its fields follows it through `/rs/vacation/stats` (see useVacationStats), the successor of the React
 * page's watch fields.
 */
export const VACATION_PAGE = definePage<
  VacationListRow,
  VacationValues,
  VacationDetail,
  typeof VACATION_METADATA
>({
  entity: "vacation",
  metadata: VACATION_METADATA,
  route: "/vacation",
  queryKey: VACATION_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Common (MenuCreator, MenuItemDefId.VACATION).
  categoryKey: "menu.common",
  titleKey: "vacation.title.list",
  columns: [
    {
      name: "employee",
      size: 170,
      cell: ({ row }) => displayName(row.original.employee),
    },
    { name: "startDate", size: 110 },
    { name: "endDate", size: 110 },
    {
      // Relative to the logged-in user; sorted by its translation (VacationEntityRest.computedSortProperties).
      id: "vacationModeString",
      labelKey: "vacation.vacationmode",
      size: 120,
      accessor: (row) => row.vacationModeString ?? "",
    },
    // Sorted as displayed, by its translation (VacationEntityRest.computedSortProperties).
    { name: "status", size: 120 },
    {
      name: "replacement",
      size: 170,
      cell: ({ row }) => displayName(row.original.replacement),
    },
    {
      id: "otherReplacements",
      labelKey: "vacation.replacement.others",
      size: 200,
      sortable: false,
      wrap: true,
      accessor: (row) =>
        (row.otherReplacements ?? []).map((e) => e.displayName).join(", "),
    },
    {
      name: "manager",
      size: 170,
      cell: ({ row }) => displayName(row.original.manager),
    },
    {
      // Computed from the period (VacationService.getVacationDays), sorted by that number on the server.
      id: "workingDaysFormatted",
      labelKey: "vacation.Days",
      size: 100,
      align: "right",
      accessor: (row) => row.workingDaysFormatted ?? "",
    },
    { name: "comment", size: 260, wrap: true },
  ],
  // The React list's `row-red`: at least one day without any replacement on duty (ConflictingVacationsCache).
  legend: [{ className: "row-red", labelKey: "vacation.conflicts" }],
  rowClassName: (row) =>
    row.conflict === true && !row.deleted ? "row-red" : undefined,
  defaultSort: { id: "startDate", desc: true },
  // The pinned period (VacationEntityRest: the vacations overlapping it) is a calendar year unless a month
  // or a free range is picked: any date typed gives its year.
  filterPeriodKinds: ["month", "year"],
  filterDefaultPeriodKind: "year",
  listActions: VacationListActions,
  edit: {
    schema: vacationSchema,
    fieldNames: VACATION_FIELDS,
    defaultValues: emptyVacationValues,
    toFormValues,
    title: (vacation) =>
      [vacation.employee?.displayName, vacation.startDateFormatted]
        .filter(Boolean)
        .join(", "),
    newTitleKey: "vacation.title.add",
    savedMessageKey: "message.successfullChanged",
    // The vacation account (still React, VacationAccountPageRest) adds an entry for the employee it shows.
    newEntryParams: ["employee"],
    // The list by default; the calendar opens an entry with `?returnTo=/calendar` (use-calendar-action.ts).
    returnTargets: [
      { route: "/vacation", labelKey: "vacation.title.list" },
      { route: "/calendar", labelKey: "menu.calendar" },
    ],
    editIntro: VacationConflictNotice,
    sections: [
      {
        id: "general",
        // The bare key of a namespace with children; the section takes its leaf (leafKeyOf).
        titleKey: "vacation",
        fields: [
          { custom: VacationEmployeeField },
          { custom: VacationWorkingDaysField },
          { custom: VacationDaysLeftField },
          { name: "startDate", startsRow: true },
          { name: "halfDayBegin" },
          { name: "endDate", startsRow: true },
          { name: "halfDayEnd" },
          { name: "manager", startsRow: true },
          { name: "special" },
          { name: "replacement", startsRow: true },
          { custom: VacationStatusField },
          { custom: VacationOtherReplacementsField, span: 3, startsRow: true },
          { name: "comment", span: 3, rows: 4 },
        ],
      },
      {
        id: "vacationsOfReplacements",
        titleKey: "vacation.vacationsOfReplacements",
        // The chart for the overview, the table for the details (and for screen readers).
        render: () => (
          <>
            <VacationOverlapChart />
            <VacationsOfReplacements />
          </>
        ),
      },
    ],
  },
});
