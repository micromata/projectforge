import { LEAVE_ACCOUNT_ENTRY_METADATA } from "@/lib/metadata/leave-account-entry.generated";
import { VACATION_ACCOUNT_QUERY_KEY } from "@/lib/rs/vacation-account";
import { definePage } from "@/lib/page-def/define-page";
import {
  leaveAccountEntrySchema,
  LEAVE_ACCOUNT_ENTRY_FIELDS,
  type LeaveAccountEntryValues,
} from "./leave-account-entry-schema";
import {
  emptyLeaveAccountEntryValues,
  toFormValues,
} from "./leave-account-entry-values";
import type {
  LeaveAccountEntryDetail,
  LeaveAccountEntryListRow,
} from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const LEAVE_ACCOUNT_ENTRY_LIST_QUERY_KEY = [
  "leaveAccountEntry",
] as const;

/**
 * The leave account entries ("Urlaubskontokorrekturen") — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the React UILayout pages of `LeaveAccountEntryPagesRest`, which it follows: the list's columns (the last
 * update added, hidden by default) and the form's employee, date, correction value and description.
 */
export const LEAVE_ACCOUNT_ENTRY_PAGE = definePage<
  LeaveAccountEntryListRow,
  LeaveAccountEntryValues,
  LeaveAccountEntryDetail,
  typeof LEAVE_ACCOUNT_ENTRY_METADATA
>({
  entity: "leaveAccountEntry",
  metadata: LEAVE_ACCOUNT_ENTRY_METADATA,
  route: "/leaveAccountEntry",
  queryKey: LEAVE_ACCOUNT_ENTRY_LIST_QUERY_KEY,
  // The vacation account (its statistics and corrections) changes with every entry saved or deleted.
  extraInvalidateKeys: [VACATION_ACCOUNT_QUERY_KEY],
  // Where the entry sits in the main menu: HR (MenuCreator, MenuItemDefId.EMPLOYEE_LEAVE_ACCOUNT_ENTRIES).
  categoryKey: "menu.hr",
  titleKey: "vacation.leaveAccountEntry.title.list",
  // Newest first, as LeaveAccountEntryDao orders them.
  defaultSort: { id: "date", desc: true },
  columns: [
    { name: "created", size: 130 },
    {
      name: "employee",
      size: 200,
      cell: ({ row }) => row.original.employee?.displayName ?? null,
    },
    { name: "date", size: 110 },
    { name: "amount", size: 130 },
    { name: "description", size: 400, wrap: true },
    { name: "lastUpdate", size: 130, hiddenByDefault: true },
  ],
  edit: {
    schema: leaveAccountEntrySchema,
    fieldNames: LEAVE_ACCOUNT_ENTRY_FIELDS,
    defaultValues: emptyLeaveAccountEntryValues,
    toFormValues,
    // Whose account and which day — the same the list shows.
    title: (entry) =>
      [entry.employee?.displayName, entry.dateFormatted]
        .filter(Boolean)
        .join(", "),
    newTitleKey: "vacation.leaveAccountEntry.title.add",
    savedMessageKey: "message.successfullChanged",
    // The vacation account (VacationAccountRest) adds an entry for the employee it shows.
    newEntryParams: ["employee"],
    // The list by default; the vacation account opens an entry with `?returnTo=/vacationAccount`
    // (VacationAccountRest.leaveAccountEntryEditUrl).
    returnTargets: [
      {
        route: "/leaveAccountEntry",
        labelKey: "vacation.leaveAccountEntry.title.list",
      },
      { route: "/vacationAccount", labelKey: "menu.vacation.leaveaccount" },
    ],
    sections: [
      {
        id: "general",
        titleKey: "vacation.leaveAccountEntry.title.heading",
        fields: [
          { name: "employee", span: 3 },
          { name: "date" },
          { name: "amount" },
          { name: "description", span: 3, rows: 3 },
        ],
      },
    ],
  },
});
