import { VACATION_METADATA } from "@/lib/metadata/vacation.generated";
import { VACATION_ACCOUNT_QUERY_KEY } from "@/lib/rs/vacation-account";
import { definePage } from "@/lib/page-def/define-page";
import { AbsenceDaysCell } from "@/components/shared/absence-chart/absence-days-bar";
import { AbsenceStartCell } from "@/components/shared/list/absence-start-cell";
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
  // The vacation account (its statistics and vacations) changes with every entry saved or deleted.
  extraInvalidateKeys: [VACATION_ACCOUNT_QUERY_KEY],
  // Where the entry sits in the main menu: Common (MenuCreator, MenuItemDefId.VACATION).
  categoryKey: "menu.common",
  titleKey: "vacation.title.list",
  columns: [
    {
      name: "employee",
      size: 170,
      cell: ({ row }) => displayName(row.original.employee),
    },
    {
      name: "startDate",
      size: 110,
      // Empty for a vacation of somebody else shown from 1 January on only: its begin isn't told.
      cell: ({ row }) => (
        <AbsenceStartCell
          startDate={row.original.startDate}
          startHidden={row.original.startHidden}
        />
      ),
    },
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
      accessor: (row) => row.otherReplacements,
    },
    {
      name: "manager",
      size: 170,
      cell: ({ row }) => displayName(row.original.manager),
    },
    {
      // Computed from the period (VacationService.getVacationDays), sorted by that number on the server. Beside
      // the number the period as a bar, red on the days without any substitute on duty.
      id: "workingDaysFormatted",
      labelKey: "vacation.Days",
      size: 220,
      accessor: (row) => row.workingDaysFormatted ?? "",
      cell: ({ row }) => (
        <AbsenceDaysCell
          formatted={row.original.workingDaysFormatted}
          row={row.original}
        />
      ),
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
    // The vacation account (VacationAccountRest) adds an entry for the employee it shows.
    newEntryParams: ["employee"],
    // "Als Abwesenheit bearbeiten" — build an availability entry from this vacation's employee, period, comment and
    // replacements and open it as a new entry (VacationEntityRest.switch2Availability →
    // AvailabilityEntityRest.cloneFromVacation). The vacation stays; the availability is named, not imported, so
    // the two features don't depend on each other in a circle.
    convert: {
      action: "switch2Availability",
      targetEntity: "availability",
      targetRoute: "/availability",
      labelKey: "vacation.switchToAvailabilityButton",
      // Nothing is converted: a new entry is opened, and a stored vacation stays as it is.
      tooltipKey: "vacation.switchToAvailabilityButton.tooltip",
    },
    // The list by default; the calendar opens an entry with `?returnTo=/calendar` (use-calendar-action.ts), the
    // vacation account with `?returnTo=/vacationAccount` (VacationAccountRest.vacationEditUrl).
    returnTargets: [
      { route: "/vacation", labelKey: "vacation.title.list" },
      { route: "/calendar", labelKey: "menu.calendar" },
      { route: "/vacationAccount", labelKey: "menu.vacation.leaveaccount" },
    ],
    editIntro: VacationConflictNotice,
    sections: [
      {
        id: "general",
        // The bare key of a namespace with children; the section takes its leaf (leafKeyOf).
        titleKey: "vacation",
        // Four columns: who, how many days and the status in one packed line, each field as wide as its value;
        // then the period with its half days and the special leave, packed the same way (so "half day" stays
        // beside its date wherever there is room); then who agrees and who substitutes; then the comment.
        columns: 4,
        fields: [
          {
            group: [
              { custom: VacationEmployeeField },
              { custom: VacationWorkingDaysField },
              { custom: VacationDaysLeftField },
              { custom: VacationStatusField },
            ],
            packed: true,
            span: 4,
          },
          {
            // Each date with its half day box as a pair of its own: the line breaks between them, not
            // inside one.
            group: [
              {
                group: [
                  { name: "startDate" },
                  {
                    name: "halfDayBegin",
                    labelKey: "vacation.halfDay",
                    hintKey: "vacation.halfDayBegin.tooltip",
                  },
                ],
                packed: true,
              },
              {
                group: [
                  { name: "endDate" },
                  {
                    name: "halfDayEnd",
                    labelKey: "vacation.halfDay",
                    hintKey: "vacation.halfDayEnd.tooltip",
                  },
                ],
                packed: true,
              },
              { name: "special" },
            ],
            packed: true,
            span: 4,
          },
          { name: "manager", startsRow: true },
          { name: "replacement" },
          { custom: VacationOtherReplacementsField, span: 2 },
          // Who is away when, right where the period and the substitutes are chosen.
          { custom: VacationOverlapChart, span: 4 },
          { name: "comment", span: 4, rows: 4 },
        ],
      },
      {
        id: "vacationsOfReplacements",
        titleKey: "vacation.vacationsOfReplacements",
        // The details of the vacations the chart above the comment shows (and the chart's text for screen
        // readers).
        render: () => <VacationsOfReplacements />,
      },
    ],
  },
});
