import type { UserLdapValuesDto, UserRightValue } from "@/lib/rs/user";

/**
 * A referenced group as the DTO carries it: the id to write back (the assignment is resolved by it,
 * `UserEntityRest.onAfterSaveOrUpdate`), the name to show.
 *
 * A type alias rather than an interface, so it satisfies the index signature of the schema's
 * `looseObject` (see `UserRefDto` in the group feature, the same shape for the same reason).
 */
export type GroupRefDto = {
  id: number;
  displayName?: string;
};

/**
 * The user as `rest/dto/User` carries it. Every optional property is `?`, not just `| null`: Spring's
 * mapper uses `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from
 * the JSON rather than null. `toFormValues` normalises that away.
 */
export interface UserDetail {
  /** null for a user that has not been saved yet (Spring assigns the id). */
  id: number | null;
  displayName?: string | null;
  username?: string | null;
  firstname?: string | null;
  nickname?: string | null;
  lastname?: string | null;
  gender?: string | null;
  organization?: string | null;
  email?: string | null;
  alternativeEmails?: string | null;
  formerEmails?: string | null;
  mobilePhone?: string | null;
  jiraUsername?: string | null;
  description?: string | null;
  hrPlanning?: boolean | null;
  deactivated?: boolean | null;
  restrictedUser?: boolean | null;
  localUser?: boolean | null;
  timeZone?: string | null;
  /** The locale's tag (`de`), absent for the default (the browser's locale). */
  locale?: string | null;
  dateFormat?: string | null;
  excelDateFormat?: string | null;
  timeNotation?: string | null;
  personalPhoneIdentifiers?: string | null;
  sshPublicKey?: string | null;
  gpgPublicKey?: string | null;
  /** The groups, sorted by name (`User.copyFrom`); administrators only. */
  assignedGroups?: GroupRefDto[] | null;
  /** The configurable rights, edit mode and administrators only (`UserEntityRest.transformFromDB`). */
  userRights?: UserRightValue[] | null;
  ldapValues?: UserLdapValuesDto | null;
  /** The initial passwords of a new user, write only. */
  password?: string | null;
  wlanPassword?: string | null;
  lastLogin?: string | null;
  lastLoginTimeAgo?: string | null;
  lastLoginFormatted?: string | null;
  lastPasswordChangeFormatted?: string | null;
  lastWlanPasswordChangeFormatted?: string | null;
  calendarExportTokenCreationTimeAgo?: string | null;
  davTokenCreationTimeAgo?: string | null;
  restClientTokenCreationTimeAgo?: string | null;
  stayLoggedInDevices?: number | null;
  stayLoggedInLastAccessTimeAgo?: string | null;
  /** The rights as text, `(ro)`/`(rw)` marked — the list column. */
  rightsAsString?: string | null;
  /*
   * Read-only flags of the request (edit mode, administrators only), no properties of the user — see
   * `User.posixConfigured` and its siblings.
   */
  posixConfigured?: boolean | null;
  sambaConfigured?: boolean | null;
  externalUserManagementSystem?: boolean | null;
  wlanPasswordSupported?: boolean | null;
  phoneIdentifiersSupported?: boolean | null;
  /** The version of the user's avatar (lib/rs/user-avatar.ts), null without one. */
  avatarVersion?: number | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface UserListRow extends UserDetail {
  id: number;
}
