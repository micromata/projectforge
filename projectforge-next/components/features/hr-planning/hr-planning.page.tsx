import { HR_PLANNING_METADATA } from "@/lib/metadata/hr-planning.generated";
import { definePage } from "@/lib/page-def/define-page";
import { HRPlanningEntriesSection } from "./entries-section";
import { HR_PLANNING_LIST_QUERY_KEY } from "./hr-planning-list.page";
import {
  hrPlanningSchema,
  HR_PLANNING_ARRAY_FIELDS,
  HR_PLANNING_FIELDS,
  type HRPlanningValues,
} from "./hr-planning-schema";
import { emptyHRPlanningValues, toFormValues } from "./hr-planning-values";
import type { HRPlanningDetail, HRPlanningListRow } from "./types";

/**
 * The form of a planned week ("Wochenplanung"): the employee, the week and its entries.
 *
 * Replaces the removed Wicket `HRPlanningEditPage`. Its list is HR_PLANNING_LIST_PAGE under the same
 * route, which lists the *entries* (`hrPlanningEntry`), so this page has no list of its own — its
 * `columns` are never shown. A new week takes the user and the week from the url (`?userId=&week=`, the
 * HR view links so), the backend normalizes the week to its Monday (`HRPlanningEntityRest.newBaseDTO`).
 */
export const HR_PLANNING_PAGE = definePage<
  HRPlanningListRow,
  HRPlanningValues,
  HRPlanningDetail,
  typeof HR_PLANNING_METADATA
>({
  entity: "hrPlanning",
  metadata: HR_PLANNING_METADATA,
  route: "/hrPlanning",
  queryKey: ["hrPlanning"],
  // A saved week changes the rows of the list, which is another entity's.
  extraInvalidateKeys: [HR_PLANNING_LIST_QUERY_KEY],
  categoryKey: "menu.projectmanagement",
  titleKey: "hr.planning.title.heading",
  columns: [],
  edit: {
    schema: hrPlanningSchema,
    fieldNames: HR_PLANNING_FIELDS,
    arrayFieldNames: HR_PLANNING_ARRAY_FIELDS,
    defaultValues: emptyHRPlanningValues,
    toFormValues,
    title: (planning) =>
      [planning.user?.displayName, planning.formattedWeekOfYear]
        .filter(Boolean)
        .join(" "),
    newTitleKey: "hr.planning.title.add",
    savedMessageKey: "message.successfullChanged",
    newEntryParams: ["userId", "week"],
    sections: [
      {
        id: "general",
        titleKey: "hr.planning.title.edit",
        highlighted: true,
        fields: [
          { name: "user" },
          // Any day of the week may be entered; the backend stores its Monday.
          { name: "week", labelKey: "calendar.week" },
        ],
      },
      {
        id: "entries",
        titleKey: "hr.planning.plannings",
        render: ({ id }) => <HRPlanningEntriesSection id={id} />,
      },
    ],
  },
});
