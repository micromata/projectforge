import { GROUP_METADATA } from "@/lib/metadata/group.generated";
import { definePage } from "@/lib/page-def/define-page";
import { AssignedUsersField } from "./assigned-users-field";
import { GroupListActions } from "./group-list-actions";
import { groupSchema, GROUP_FIELDS, type GroupValues } from "./group-schema";
import { emptyGroupValues, toFormValues } from "./group-values";
import { LdapGidField } from "./ldap-gid-field";
import { MemberEmailsField } from "./member-emails-field";
import type { GroupDetail, GroupListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const GROUP_LIST_QUERY_KEY = ["group"] as const;

/**
 * The whole group page — list and edit — as data (see lib/page-def/types.ts).
 *
 * The columns are those of `GroupPagesRest.createListLayout` in its order, the form is its
 * `createEditLayout`; every label and every rule comes from GroupDO through the generated metadata.
 * Declared here is order and width, plus the three fields the declaration cannot describe because
 * GroupDO has no such property: the members, the LDAP gid and the computed mail addresses.
 *
 * One section and no attachments: the entity has no `jcrPath`. Its change history is a tab of its
 * own, which `GROUP_METADATA.historizable` says.
 */
export const GROUP_PAGE = definePage<
  GroupListRow,
  GroupValues,
  GroupDetail,
  typeof GROUP_METADATA
>({
  entity: "group",
  metadata: GROUP_METADATA,
  route: "/group",
  queryKey: GROUP_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Administration > Groups (MenuCreator, GROUP_LIST).
  categoryKey: "menu.administration",
  titleKey: "group.title.list",
  columns: [
    { name: "name", size: 220, pinned: "left" },
    // Wrapped, the three of them, as the legacy list wraps them (`wrapText`): an organization, a
    // description and a list of members are longer than a column and worth reading whole.
    { name: "organization", size: 200, wrap: true },
    { name: "description", size: 320, wrap: true },
    {
      // The members, joined as the legacy list shows them (`SHOW_LIST_OF_DISPLAYNAMES`). No sorting:
      // the backend orders by entity property, and a collection is none.
      id: "assignedUsers",
      labelKey: "group.assignedUsers",
      accessor: (row) =>
        row.assignedUsers?.map((user) => user.displayName).join(", "),
      size: 320,
      sortable: false,
      wrap: true,
    },
    {
      // What the LDAP sync wrote — only worth a column where posix accounts are in use and only for an
      // administrator, which is what the backend answers (`GroupPagesRest.addVariablesForListPage`).
      name: "ldapValues",
      size: 200,
      visible: (ctx) => ctx.variables?.ldapPosixConfigured === true,
    },
  ],
  listActions: GroupListActions,
  edit: {
    schema: groupSchema,
    fieldNames: GROUP_FIELDS,
    arrayFieldNames: ["assignedUsers"],
    defaultValues: emptyGroupValues,
    toFormValues,
    // The name is what identifies a group — the same string the list shows.
    title: (group) => group.name ?? "",
    newTitleKey: "group.title.add",
    savedMessageKey: "message.successfullChanged",
    clone: true,
    sections: [
      {
        id: "general",
        titleKey: "group._",
        fields: [
          { name: "name", span: 2 },
          // Whether the group is exported to an external user management system at all — read before
          // anything below it, hence beside the name and not further down.
          { name: "localGroup", hintKey: "group.localGroup.tooltip" },
          { name: "organization", span: 2 },
          { name: "groupOwner" },
          { custom: AssignedUsersField, span: 3 },
          { name: "description", span: 3, rows: 4 },
          { custom: MemberEmailsField, span: 3 },
        ],
      },
      {
        // Its own card, as the legacy layout gives it its own fieldset (`UIFieldset(title = "ldap")`),
        // and only where the backend says posix accounts are in use — see LdapGidField.
        // `ldap._`, because `ldap` is a label and the parent of `ldap.gidNumber` too (see leafKeyOf).
        id: "ldap",
        titleKey: "ldap._",
        visible: ({ data }) => data?.ldapPosixConfigured === true,
        fields: [{ custom: LdapGidField, span: 3 }],
      },
    ],
  },
});
