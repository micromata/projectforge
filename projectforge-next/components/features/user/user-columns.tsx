import { HugeiconsIcon } from "@hugeicons/react";
import { UserLock01Icon } from "@hugeicons/core-free-icons";
import type { PF_USER_METADATA } from "@/lib/metadata/pf-user.generated";
import type { ColumnDeclaration } from "@/lib/page-def/types";
import type { UserListRow } from "./types";

/** Only an administrator gets the columns beyond the names and keys (`UserEntityRest.addVariablesForListPage`). */
const adminOnly = (ctx: { variables?: Record<string, unknown> }) =>
  ctx.variables?.isAdmin === true;

/**
 * The columns of the user list, following the former server side layout (`UserPagesRest`): everyone
 * sees the names and public keys of the users sharing a group with them, an administrator everything.
 */
export const USER_COLUMNS: ColumnDeclaration<
  UserListRow,
  typeof PF_USER_METADATA
>[] = [
  { name: "username", size: 160, pinned: "left" },
  {
    // A lock for a deactivated user, as the legacy list shows it (`UIIconType.USER_LOCK`).
    name: "deactivated",
    size: 80,
    visible: adminOnly,
    cell: ({ getValue }) =>
      getValue() === true ? (
        <HugeiconsIcon icon={UserLock01Icon} size={16} aria-hidden />
      ) : null,
  },
  {
    id: "lastLoginTimeAgo",
    labelKey: "login.lastLogin",
    accessor: (row) => row.lastLoginTimeAgo,
    size: 150,
    sortable: false,
    visible: adminOnly,
  },
  // Both audit columns are an administrator's, so declared here: every list appends them otherwise.
  { name: "created", size: 150, visible: adminOnly, hiddenByDefault: true },
  { name: "lastUpdate", size: 150, visible: adminOnly },
  { name: "lastname", size: 160 },
  { name: "firstname", size: 160 },
  {
    name: "personalPhoneIdentifiers",
    size: 160,
    visible: adminOnly,
    hiddenByDefault: true,
  },
  { name: "description", size: 220, wrap: true, visible: adminOnly },
  { name: "mobilePhone", size: 150, visible: adminOnly },
  {
    // The groups, as the legacy list shows them (`SHOW_LIST_OF_DISPLAYNAMES`), a deleted one struck
    // through (see RefList). No sorting: the backend orders by entity property, and a collection is none.
    id: "assignedGroups",
    labelKey: "user.assignedGroups",
    accessor: (row) => row.assignedGroups,
    size: 320,
    sortable: false,
    wrap: true,
    visible: adminOnly,
  },
  {
    id: "rightsAsString",
    labelKey: "access.rights",
    accessor: (row) => row.rightsAsString,
    size: 320,
    sortable: false,
    wrap: true,
    visible: adminOnly,
  },
  {
    // What the LDAP sync wrote — only where posix accounts are in use (and for an administrator).
    id: "ldapValues",
    labelKey: "user.ldapValues",
    accessor: (row) => row.ldapValues?.asString,
    size: 220,
    sortable: false,
    wrap: true,
    // posixConfigured alone holds for everyone in development mode.
    visible: (ctx) => adminOnly(ctx) && ctx.variables?.posixConfigured === true,
  },
  { name: "gpgPublicKey", size: 200, wrap: true, hiddenByDefault: true },
  { name: "sshPublicKey", size: 200, wrap: true, hiddenByDefault: true },
];
