import { MERLIN_TEMPLATE_METADATA } from "@/lib/metadata/merlin-template.generated";
import { definePage } from "@/lib/page-def/define-page";
import { attachmentsColumn } from "@/components/shared/attachments/attachments-column";
import {
  MerlinAccessGroupMembers,
  MerlinAccessGroupsField,
  MerlinAccessUsersField,
  MerlinAdminsField,
} from "./merlin-access-fields";
import { MerlinAttachments } from "./merlin-attachments";
import { MerlinEditHeaderActions } from "./merlin-edit-header-actions";
import {
  MerlinDataTransferUsageField,
  MerlinFileFields,
} from "./merlin-file-fields";
import { MerlinListActions } from "./merlin-list-actions";
import { MERLIN_ROUTE, merlinExecuteRoute } from "./merlin-routes";
import {
  MERLIN_ARRAY_FIELDS,
  MERLIN_FIELDS,
  merlinSchema,
  type MerlinValues,
} from "./merlin-schema";
import { emptyMerlinValues, toFormValues } from "./merlin-values";
import { MerlinVariablesField } from "./merlin-variables-field";
import type { MerlinDetail, MerlinListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const MERLIN_LIST_QUERY_KEY = ["merlin"] as const;

/**
 * The Word® templates of the plugin merlin — list and admin form — as data (see lib/page-def/types.ts).
 *
 * A row opens the template's *execution* (`/merlin/{id}`, the default of `openEntry`), which is what the
 * list is used for; the admin form is `/merlin/{id}/edit`, linked from there and beside the form's heading.
 */
export const MERLIN_PAGE = definePage<
  MerlinListRow,
  MerlinValues,
  MerlinDetail,
  typeof MERLIN_TEMPLATE_METADATA
>({
  entity: "merlin",
  metadata: MERLIN_TEMPLATE_METADATA,
  route: MERLIN_ROUTE,
  queryKey: MERLIN_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Miscellaneous (MerlinPlugin registers it under MISC).
  categoryKey: "menu.misc",
  titleKey: "plugins.merlin.title.list",
  defaultSort: { id: "lastUpdate", desc: true },
  columns: [
    { name: "created", size: 130 },
    { name: "lastUpdate", size: 130 },
    { name: "name", size: 220, className: "font-semibold" },
    { name: "description", size: 300 },
    attachmentsColumn<MerlinListRow>(),
    {
      id: "adminsAsString",
      labelKey: "plugins.merlin.admins._",
      // The users by name, each struck through when no longer active (see RefList).
      accessor: (row) => row.admins,
      size: 180,
    },
    {
      id: "accessUsersAsString",
      labelKey: "plugins.merlin.accessUsers._",
      accessor: (row) => row.accessUsers,
      size: 180,
    },
    {
      id: "accessGroupsAsString",
      labelKey: "plugins.merlin.accessGroups._",
      accessor: (row) => row.accessGroupsAsString ?? "",
      size: 180,
    },
  ],
  listActions: MerlinListActions,
  edit: {
    schema: merlinSchema,
    fieldNames: MERLIN_FIELDS,
    arrayFieldNames: MERLIN_ARRAY_FIELDS,
    defaultValues: emptyMerlinValues,
    toFormValues,
    title: (template) => template.name ?? "",
    newTitleKey: "plugins.merlin.title.add",
    savedMessageKey: "message.successfullChanged",
    clone: true,
    crossLinks: [
      {
        labelKey: "plugins.merlin.templateExecutor.execute",
        href: (template) =>
          template.id != null ? merlinExecuteRoute(template.id) : null,
        prominent: true,
      },
    ],
    headerTrailing: () => <MerlinEditHeaderActions />,
    sections: [
      {
        id: "general",
        titleKey: "plugins.merlin.title.heading",
        fields: [
          { name: "name", span: 2 },
          { name: "fileNamePattern", span: 2 },
          { custom: MerlinFileFields, span: 2 },
          {
            group: [
              { name: "stronglyRestrictedFilenames" },
              { name: "pdfExport" },
              { custom: MerlinDataTransferUsageField },
            ],
            span: 2,
          },
          { name: "description", span: 4 },
        ],
      },
      {
        id: "variables",
        titleKey: "plugins.merlin.variables._",
        fields: [{ custom: MerlinVariablesField, span: 4 }],
      },
      {
        id: "access",
        titleKey: "access.title.heading",
        fields: [
          { custom: MerlinAdminsField, span: 2 },
          { custom: MerlinAccessUsersField, span: 2 },
          { custom: MerlinAccessGroupsField, span: 2 },
          { custom: MerlinAccessGroupMembers, span: 2 },
        ],
      },
      {
        id: "attachments",
        titleKey: "attachment.list",
        visible: ({ data }) => data?.id != null,
        render: ({ id }) => <MerlinAttachments id={id} />,
      },
    ],
  },
});
