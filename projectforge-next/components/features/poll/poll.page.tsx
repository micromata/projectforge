import { POLL_METADATA } from "@/lib/metadata/poll.generated";
import { definePage } from "@/lib/page-def/define-page";
import {
  PollAttendeeGroupsField,
  PollAttendeeUsersField,
  PollFullAccessGroupsField,
  PollFullAccessUsersField,
} from "./poll-access-fields";
import { PollEditIntro } from "./poll-edit-intro";
import { PollHeaderActions, POLL_LIST_QUERY_KEY } from "./poll-header-actions";
import {
  PollInvitationSubjectField,
  PollInvitationTextField,
} from "./poll-invitation-fields";
import { PollQuestionsEditor } from "./poll-questions-editor";
import { PollResultsPanel, RESULTS_TAB_ID } from "./poll-results-panel";
import { pollSchema, POLL_FIELDS, type PollValues } from "./poll-schema";
import { emptyPollValues, toFormValues } from "./poll-values";
import type { PollDetail, PollListRow, PollRef } from "./types";

/** The display names of users or groups, as one cell. */
function names(refs: PollRef[] | undefined): string {
  return (refs ?? []).map((ref) => ref.displayName ?? "???").join(", ");
}

/**
 * The polls — list and edit — as data (see lib/page-def/types.ts). Replaces the React UILayout pages of
 * `PollPageRest`. Answering is a page of its own (`/poll/<id>/answer`, see PollAnswerPage), and the results are a
 * tab of the edit page.
 *
 * A row opens the edit page for whoever may change the poll (the owner and the users with full access), the
 * answer page for an attendee.
 */
export const POLL_PAGE = definePage<
  PollListRow,
  PollValues,
  PollDetail,
  typeof POLL_METADATA
>({
  entity: "poll",
  metadata: POLL_METADATA,
  route: "/poll",
  queryKey: POLL_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Misc (MenuCreator, MenuItemDefId.POLL).
  categoryKey: "menu.misc",
  titleKey: "poll.title.list",
  defaultSort: { id: "deadline", desc: true },
  onRowClick: (row) =>
    row.fullAccess || row.template ? undefined : `/poll/${row.id}/answer`,
  columns: [
    { name: "title", size: 260, className: "font-semibold" },
    { name: "location", size: 160 },
    {
      name: "owner",
      size: 170,
      cell: ({ row }) => row.original.owner?.displayName ?? null,
    },
    { name: "deadline", size: 110 },
    { name: "state", size: 100 },
    {
      // The attendees are csv lists of ids in the database, resolved by the DTO: nothing to sort by.
      id: "attendees",
      labelKey: "poll.attendees._",
      accessor: (row) =>
        [names(row.attendeeGroups), names(row.attendeeUsers)]
          .filter(Boolean)
          .join(", "),
      sortable: false,
      size: 260,
      wrap: true,
    },
    { name: "lastUpdate", size: 130 },
  ],
  edit: {
    schema: pollSchema,
    fieldNames: POLL_FIELDS,
    arrayFieldNames: [
      "questions",
      "attendeeUsers",
      "attendeeGroups",
      "fullAccessUsers",
      "fullAccessGroups",
    ],
    defaultValues: emptyPollValues,
    toFormValues,
    title: (poll) => poll.title ?? "",
    newTitleKey: "poll.title.add",
    savedMessageKey: "message.successfullChanged",
    clone: true,
    // A finished poll is shown as it is: it can't be changed anymore (PollDao refuses the update).
    readOnlyWithoutWriteAccess: true,
    headerTrailing: (poll) => <PollHeaderActions poll={poll} />,
    editIntro: PollEditIntro,
    sections: [
      {
        id: "general",
        titleKey: "poll.heading.general",
        fields: [
          { name: "title", span: 2 },
          { name: "owner", readOnly: true },
          { name: "location", span: 2 },
          { name: "deadline", hintKey: "poll.deadline.tooltip" },
          { name: "description", span: 3, rows: 3 },
          { name: "template" },
        ],
      },
      {
        id: "questions",
        titleKey: "poll.heading.questions",
        fields: [{ custom: PollQuestionsEditor, span: 3 }],
      },
      {
        id: "access",
        titleKey: "poll.heading.access",
        columns: 4,
        fields: [
          { custom: PollAttendeeUsersField, span: 2 },
          { custom: PollAttendeeGroupsField, span: 2 },
          { custom: PollFullAccessUsersField, span: 2 },
          { custom: PollFullAccessGroupsField, span: 2 },
        ],
      },
      {
        // Sent on the first save only, so there is nothing to show for a stored poll.
        id: "invitation",
        titleKey: "poll.heading.invitation",
        visible: ({ data }) => data == null || data.id == null,
        fields: [
          { custom: PollInvitationSubjectField, span: 3 },
          { custom: PollInvitationTextField, span: 3 },
        ],
      },
    ],
    extraTabs: [
      {
        id: RESULTS_TAB_ID,
        labelKey: "poll.results._",
        component: PollResultsPanel,
      },
    ],
  },
});
