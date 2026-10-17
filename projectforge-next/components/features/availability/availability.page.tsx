import { AVAILABILITY_METADATA } from "@/lib/metadata/availability.generated";
import { definePage } from "@/lib/page-def/define-page";
import { AvailabilityConflictNotice } from "./availability-conflict-notice";
import {
  AvailabilityInternalCommentField,
  AvailabilitySubstitutesField,
  AvailabilityTypeField,
  AvailabilityUserField,
} from "./availability-fields";
import { AvailabilityListActions } from "./availability-list-actions";
import { AvailabilitySubstituteAbsences } from "./availability-substitute-absences";
import { AvailabilitySubstituteChart } from "./availability-substitute-chart";
import {
  availabilitySchema,
  AVAILABILITY_FIELDS,
  type AvailabilityValues,
} from "./availability-schema";
import { emptyAvailabilityValues, toFormValues } from "./availability-values";
import type { AvailabilityDetail, AvailabilityListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const AVAILABILITY_LIST_QUERY_KEY = ["availability"] as const;

/**
 * The availability entries — list and edit — as data (see lib/page-def/types.ts). Replaces the React UILayout
 * pages of `AvailabilityPagesRest`: periods of reduced availability (parental leave, workation, …) of users —
 * employees and freelancers alike — with their type, reachability and location, a comment for everybody who may
 * see the entry and an internal one for the user and HR staff only, and optional substitutes: a conflict if all of them
 * are away on a day of it.
 */
export const AVAILABILITY_PAGE = definePage<
  AvailabilityListRow,
  AvailabilityValues,
  AvailabilityDetail,
  typeof AVAILABILITY_METADATA
>({
  entity: "availability",
  metadata: AVAILABILITY_METADATA,
  route: "/availability",
  queryKey: AVAILABILITY_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Common (MenuCreator, MenuItemDefId.AVAILABILITY).
  categoryKey: "menu.common",
  titleKey: "availability.title.list",
  columns: [
    {
      name: "user",
      size: 170,
      cell: ({ row }) => row.original.user?.displayName ?? null,
    },
    { name: "startDate", size: 110 },
    { name: "endDate", size: 110 },
    { name: "type", size: 140 },
    // Sorted as displayed, by their translation (AvailabilityEntityRest.computedSortProperties).
    { name: "status", size: 170 },
    { name: "location", size: 110 },
    { name: "comment", size: 260, wrap: true },
    {
      id: "substitutes",
      labelKey: "availability.substitutes",
      size: 200,
      sortable: false,
      wrap: true,
      accessor: (row) => row.substitutesAsString ?? "",
    },
  ],
  // At least one day without any substitute on duty (ConflictingAvailabilitiesCache), as in the vacation list.
  legend: [{ className: "row-red", labelKey: "availability.conflicts" }],
  rowClassName: (row) =>
    row.conflict === true && !row.deleted ? "row-red" : undefined,
  defaultSort: { id: "startDate", desc: true },
  // The pinned period (AvailabilityEntityRest: the entries overlapping it) is a calendar year unless a month or a
  // free range is picked.
  filterPeriodKinds: ["month", "year"],
  filterDefaultPeriodKind: "year",
  // The team vacation calendar, with the availability entries shown.
  listActions: AvailabilityListActions,
  edit: {
    schema: availabilitySchema,
    fieldNames: AVAILABILITY_FIELDS,
    defaultValues: emptyAvailabilityValues,
    toFormValues,
    title: (availability) =>
      [availability.user?.displayName, availability.type]
        .filter(Boolean)
        .join(", "),
    newTitleKey: "availability.title.add",
    savedMessageKey: "message.successfullChanged",
    // HR staff may preset the user (`?user=<id>`, AvailabilityEntityRest.newBaseDTO).
    newEntryParams: ["user"],
    // The warning of a day without any substitute on duty; a start without an end makes a one-day period, as in
    // the vacation form.
    editIntro: AvailabilityConflictNotice,
    // The list by default; the calendar opens an entry with `?returnTo=/calendar` (use-calendar-action.ts).
    returnTargets: [
      { route: "/availability", labelKey: "availability.title.list" },
      { route: "/calendar", labelKey: "menu.calendar" },
    ],
    sections: [
      {
        id: "general",
        titleKey: "availability.title",
        fields: [
          { custom: AvailabilityUserField },
          { name: "status" },
          { name: "startDate", startsRow: true },
          { name: "endDate" },
          { custom: AvailabilityTypeField, startsRow: true },
          { name: "location" },
          {
            name: "comment",
            span: 2,
            rows: 3,
            hintKey: "availability.comment.tooltip",
          },
          { custom: AvailabilityInternalCommentField, span: 2 },
          { custom: AvailabilitySubstitutesField, span: 2, startsRow: true },
          // Who is away when, right where the substitutes are chosen.
          { custom: AvailabilitySubstituteChart, span: 4 },
        ],
      },
      {
        id: "substituteAbsences",
        titleKey: "availability.substitutes.absences",
        // The details of the absences the chart shows (and the chart's text for screen readers).
        render: () => <AvailabilitySubstituteAbsences />,
      },
    ],
  },
});
