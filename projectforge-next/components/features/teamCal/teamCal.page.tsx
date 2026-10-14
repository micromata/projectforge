import { TEAM_CAL_METADATA } from "@/lib/metadata/team-cal.generated";
import { definePage } from "@/lib/page-def/define-page";
import {
  TeamCalFullAccessGroupsField,
  TeamCalFullAccessUsersField,
  TeamCalLeaveDaysGroupsField,
  TeamCalLeaveDaysUsersField,
  TeamCalMinimalAccessGroupsField,
  TeamCalMinimalAccessUsersField,
  TeamCalReadonlyAccessGroupsField,
  TeamCalReadonlyAccessUsersField,
} from "./team-cal-access-fields";
import { TeamCalExternalSubscription } from "./team-cal-external-subscription";
import { TeamCalListActions } from "./team-cal-list-actions";
import {
  teamCalSchema,
  TEAM_CAL_FIELDS,
  type TeamCalValues,
} from "./team-cal-schema";
import { TeamCalSubscriptionInfo } from "./team-cal-subscription-info";
import { emptyTeamCalValues, toFormValues } from "./team-cal-values";
import type { TeamCalDetail, TeamCalListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const TEAM_CAL_LIST_QUERY_KEY = ["teamCal"] as const;

/**
 * The team calendars — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the React UILayout pages of `TeamCalPagesRest`, which it follows: the list's columns in their
 * order with the export feeds in its export menu, and the form's title, description and external
 * subscription, the access rights of users and groups, the users and groups whose leave days the calendar
 * shows, and — for a stored calendar — its feed for calendar apps.
 */
export const TEAM_CAL_PAGE = definePage<
  TeamCalListRow,
  TeamCalValues,
  TeamCalDetail,
  typeof TEAM_CAL_METADATA
>({
  entity: "teamCal",
  metadata: TEAM_CAL_METADATA,
  route: "/teamCal",
  queryKey: TEAM_CAL_LIST_QUERY_KEY,
  // The calendar page and the calendar selects of the event form and the ICS import list the calendars, too.
  extraInvalidateKeys: [["calendar"], ["teamEvent", "calendars"]],
  // Where the entry sits in the main menu: Common (MenuCreator, MenuItemDefId.CALENDAR_LIST).
  categoryKey: "menu.common",
  titleKey: "plugins.teamcal.title.list",
  defaultSort: { id: "title", desc: false },
  columns: [
    { name: "title", size: 220, className: "font-semibold" },
    // The url without its credentials; the url itself is sent to the form of users allowed to save only.
    { name: "externalSubscriptionUrlAnonymized", size: 240 },
    { name: "description", size: 300, wrap: true },
    {
      name: "owner",
      size: 170,
      cell: ({ row }) => row.original.owner?.displayName ?? null,
    },
    {
      // The logged-in user's access, computed by the DTO (TeamCal.copyFrom).
      id: "accessStatusString",
      labelKey: "plugins.teamcal.access._",
      accessor: (row) => row.accessStatusString ?? "",
      sortable: false,
      size: 140,
    },
  ],
  listActions: TeamCalListActions,
  edit: {
    schema: teamCalSchema,
    fieldNames: TEAM_CAL_FIELDS,
    arrayFieldNames: [
      "fullAccessUsers",
      "fullAccessGroups",
      "readonlyAccessUsers",
      "readonlyAccessGroups",
      "minimalAccessUsers",
      "minimalAccessGroups",
      "includeLeaveDaysForUsers",
      "includeLeaveDaysForGroups",
    ],
    defaultValues: emptyTeamCalValues,
    toFormValues,
    title: (cal) => cal.title ?? "",
    newTitleKey: "plugins.teamcal.title.add",
    savedMessageKey: "message.successfullChanged",
    crossLinks: [
      {
        // Only into a calendar the user may write into (TeamCalDao.writableCalendars): no external subscription.
        labelKey: "plugins.teamcal.import.ics.title",
        href: (cal) =>
          cal.id != null &&
          !cal.externalSubscription &&
          (cal.accessStatus === "OWNER" || cal.accessStatus === "FULL_ACCESS")
            ? `/teamCalImport?teamCalId=${cal.id}`
            : null,
        prominent: true,
      },
    ],
    sections: [
      {
        id: "general",
        titleKey: "plugins.teamcal.title.heading",
        fields: [
          { name: "title", span: 2 },
          { name: "owner" },
          { name: "description", span: 3, rows: 3 },
          { custom: TeamCalExternalSubscription, span: 3 },
        ],
      },
      {
        id: "access",
        titleKey: "plugins.teamcal.access.title",
        columns: 4,
        fields: [
          { custom: TeamCalFullAccessUsersField, span: 2 },
          { custom: TeamCalFullAccessGroupsField, span: 2 },
          { custom: TeamCalReadonlyAccessUsersField, span: 2 },
          { custom: TeamCalReadonlyAccessGroupsField, span: 2 },
          { custom: TeamCalMinimalAccessUsersField, span: 2 },
          { custom: TeamCalMinimalAccessGroupsField, span: 2 },
        ],
      },
      {
        id: "vacation",
        titleKey: "vacation._",
        columns: 4,
        fields: [
          { custom: TeamCalLeaveDaysUsersField, span: 2 },
          { custom: TeamCalLeaveDaysGroupsField, span: 2 },
        ],
      },
      {
        // The feed of a stored calendar only; its urls come with the calendar (TeamCal.subscriptionInfo).
        id: "subscription",
        titleKey: "plugins.teamcal.subscription._",
        visible: ({ data }) => data?.subscriptionInfo != null,
        fields: [{ custom: TeamCalSubscriptionInfo, span: 3 }],
      },
    ],
  },
});
