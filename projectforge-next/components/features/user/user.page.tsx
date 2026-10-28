import { INACTIVE_ROW_CLASS } from "@/lib/dynamic/grid/row-class";
import { PF_USER_METADATA } from "@/lib/metadata/pf-user.generated";
import { definePage } from "@/lib/page-def/define-page";
import { UserAccessFields } from "./user-access-fields";
import { UserAvatarSection } from "./user-avatar-section";
import { USER_COLUMNS } from "./user-columns";
import { UserGroupsField } from "./user-groups-field";
import { UserLdapFields } from "./user-ldap-fields";
import { UserListActions } from "./user-list-actions";
import { UserPasswordActions } from "./user-password-actions";
import { UserPasswordFields } from "./user-password-fields";
import { UserRightsField } from "./user-rights-field";
import { userSchema, USER_FIELDS, type UserValues } from "./user-schema";
import { UserSettingsFields } from "./user-settings-fields";
import { emptyUserValues, toFormValues } from "./user-values";
import type { UserDetail, UserListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const USER_LIST_QUERY_KEY = ["user"] as const;

/**
 * The whole user page — list and edit — as data (see lib/page-def/types.ts).
 *
 * The columns follow the former server side layout of the user list (`UserPagesRest`): everyone sees
 * the names and public keys of the users sharing a group with them, an administrator everything. Only an
 * administrator may change a user, so the form is theirs alone.
 *
 * Declared in the form are the properties of PFUserDO; custom are the parts the declaration cannot
 * describe — the settings with their server side choices, the tokens and devices with their immediate
 * actions, the groups and the rights depending on them, and the LDAP values.
 */
export const USER_PAGE = definePage<
  UserListRow,
  UserValues,
  UserDetail,
  typeof PF_USER_METADATA
>({
  entity: "user",
  metadata: PF_USER_METADATA,
  route: "/user",
  queryKey: USER_LIST_QUERY_KEY,
  // Administration > Users (MenuCreator, USER_LIST).
  categoryKey: "menu.administration",
  titleKey: "user.title.list",
  columns: USER_COLUMNS,
  listActions: UserListActions,
  // A deactivated user reads struck through (thin, grey), as the selection boxes strike them
  // (DEACTIVATED_REF_CLASS), set apart from a deleted one.
  inactiveLabelKey: "user.legend.deactivated",
  rowClassName: (row) => (row.deactivated ? INACTIVE_ROW_CLASS : undefined),
  edit: {
    schema: userSchema,
    fieldNames: USER_FIELDS,
    arrayFieldNames: ["assignedGroups", "userRights"],
    defaultValues: emptyUserValues,
    toFormValues,
    title: (user) => user.displayName ?? user.username ?? "",
    newTitleKey: "user.title.add",
    savedMessageKey: "message.successfullChanged",
    autoFocus: "username",
    clone: true,
    // The password change, which an administrator comes here for most, at the very top.
    headerTrailing: (user) => <UserPasswordActions user={user} />,
    sections: [
      {
        id: "account",
        titleKey: "user.myAccount.section.account",
        fields: [
          { name: "username" },
          { name: "firstname" },
          { name: "lastname" },
          { name: "nickname" },
          { name: "gender" },
          { name: "organization" },
          { name: "email" },
          { name: "alternativeEmails" },
          { name: "formerEmails" },
          { name: "mobilePhone" },
          { name: "jiraUsername" },
          { custom: UserPasswordFields, span: 3 },
          { name: "hrPlanning" },
          { name: "deactivated" },
          { name: "restrictedUser" },
          { name: "description", span: 3, rows: 3 },
        ],
      },
      {
        // Changed at once, not with the save: so only for an existing user.
        id: "avatar",
        titleKey: "user.avatar._",
        visible: ({ data }) => data?.id != null,
        fields: [{ custom: UserAvatarSection, span: 3 }],
      },
      {
        id: "settings",
        titleKey: "user.myAccount.section.settings",
        fields: [{ custom: UserSettingsFields, span: 3 }],
      },
      {
        // Tokens and devices belong to an existing user only.
        id: "tokens",
        titleKey: "user.myAccount.section.tokens",
        visible: ({ data }) => data?.id != null,
        fields: [{ custom: UserAccessFields, span: 3 }],
      },
      {
        id: "rights",
        titleKey: "access.rights",
        fields: [
          { custom: UserGroupsField, span: 3 },
          { custom: UserRightsField, span: 3 },
        ],
      },
      {
        id: "publicKeys",
        titleKey: "user.myAccount.section.publicKeys",
        fields: [
          { name: "sshPublicKey", span: 3, rows: 4 },
          { name: "gpgPublicKey", span: 3, rows: 4 },
        ],
      },
      {
        // Its own card, as the legacy layout gives it its own fieldset, and only where posix or samba
        // accounts are in use. `ldap._`, because `ldap` is a label and the parent of `ldap.uidNumber` too.
        id: "ldap",
        titleKey: "ldap._",
        visible: ({ data }) =>
          data?.posixConfigured === true || data?.sambaConfigured === true,
        fields: [{ custom: UserLdapFields, span: 3 }],
      },
    ],
  },
});
