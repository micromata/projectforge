import { JiraLinkedText } from "@/components/shared/jira/jira-linked-text";
import { HR_PLANNING_ENTRY_METADATA } from "@/lib/metadata/hr-planning-entry.generated";
import { defineListPage } from "@/lib/page-def/define-page";
import { Hours } from "./hours";
import { HRPlanningStatisticsLine } from "./hr-planning-statistics-line";
import type { HRPlanningListRow, HRPlanningListStatistics } from "./types";

/** React Query key of the list, so a write from the form of a week refreshes it. */
export const HR_PLANNING_LIST_QUERY_KEY = ["hrPlanningEntry"] as const;

/** A narrow hour column, as the legacy list had one per day. */
function hourColumn(
  name:
    | "unassignedHours"
    | "mondayHours"
    | "tuesdayHours"
    | "wednesdayHours"
    | "thursdayHours"
    | "fridayHours"
    | "weekendHours"
) {
  return {
    name,
    size: name === "unassignedHours" ? 90 : 60,
    filterKind: null,
    align: "right",
    cell: ({ row }: { row: { original: HRPlanningListRow } }) => (
      <Hours value={row.original[name]} />
    ),
  } as const;
}

/**
 * The HR planning list ("Wochenplanung"): the entries of the planned weeks the filter matches, one row
 * each, or one per week and project group with "grouped" on (`HRPlanningEntryDao.groupAndFilter`).
 *
 * Replaces the removed Wicket `HRPlanningListPage`, whose columns it shows in its order. A row is an
 * entry, but what is edited is the week (HR_PLANNING_PAGE, under the same route): a click opens the week
 * of the row ([foreignEdit]), and only for who may write the HR planning (`listUpdateAccess`).
 */
export const HR_PLANNING_LIST_PAGE = defineListPage<
  HRPlanningListRow,
  typeof HR_PLANNING_ENTRY_METADATA
>({
  entity: "hrPlanningEntry",
  metadata: HR_PLANNING_ENTRY_METADATA,
  route: "/hrPlanning",
  queryKey: HR_PLANNING_LIST_QUERY_KEY,
  foreignEdit: true,
  onRowClick: (row) =>
    row.planningId != null ? `/hrPlanning/${row.planningId}` : undefined,
  // A planning is a week, so the period filter pages a week at a time first (as the time sheets).
  filterPeriodKinds: ["week", "month", "termThreeMonths", "termYear"],
  // Project management > HR planning list (MenuItemDefId.HR_PLANNING_LIST under projectManagementMenu).
  categoryKey: "menu.projectmanagement",
  titleKey: "menu.hrPlanningList",
  // The legacy list's columns. The ones of the week (user, year, week, its total) and of the project
  // (customer) are no properties of the entry: the backend could not order by them, so they don't sort
  // — the list comes ordered by week (newest first) and user (`HRPlanningEntryEntityRest`).
  columns: [
    {
      id: "user",
      labelKey: "timesheet.user",
      accessor: (row) => row.user?.displayName ?? "",
      size: 160,
      sortable: false,
    },
    {
      id: "formattedWeekOfYear",
      labelKey: "calendar.weekOfYearShortLabel",
      accessor: (row) => row.formattedWeekOfYear ?? "",
      size: 80,
      sortable: false,
      filterKind: null,
    },
    {
      id: "kunde",
      labelKey: "fibu.kunde",
      accessor: (row) => row.kunde ?? "",
      size: 160,
      sortable: false,
    },
    {
      id: "projektNameOrStatus",
      labelKey: "fibu.projekt",
      accessor: (row) => row.projektNameOrStatus ?? "",
      size: 200,
      className: "font-semibold",
      sortable: false,
    },
    { name: "priority", size: 90 },
    {
      name: "probability",
      size: 60,
      cell: ({ row }) =>
        row.original.probability != null ? `${row.original.probability}%` : "",
    },
    {
      id: "planningTotalHours",
      labelKey: "hr.planning.total",
      accessor: (row) => row.planningTotalHours ?? null,
      size: 70,
      align: "right",
      sortable: false,
      filterKind: null,
      cell: ({ row }) => <Hours value={row.original.planningTotalHours} />,
    },
    {
      id: "totalHours",
      labelKey: "hr.planning.sum",
      accessor: (row) => row.totalHours ?? null,
      size: 70,
      align: "right",
      sortable: false,
      filterKind: null,
      cell: ({ row }) => (
        <Hours value={row.original.totalHours} className="font-medium" />
      ),
    },
    hourColumn("unassignedHours"),
    hourColumn("mondayHours"),
    hourColumn("tuesdayHours"),
    hourColumn("wednesdayHours"),
    hourColumn("thursdayHours"),
    hourColumn("fridayHours"),
    hourColumn("weekendHours"),
    {
      name: "description",
      size: 360,
      wrap: true,
      // JIRA issue keys become links, as in the legacy list.
      cell: ({ row }) => <JiraLinkedText text={row.original.description} />,
    },
  ],
  // The sum of the planned hours over the rows of the filter, above the table.
  statistics: ({ statistics, isFetching }) => (
    <HRPlanningStatisticsLine
      statistics={statistics as HRPlanningListStatistics | undefined}
      isFetching={isFetching}
    />
  ),
});
