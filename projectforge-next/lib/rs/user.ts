/**
 * The calls of the user page (`UserEntityRest`, `UserServicesRest`) beside the list, the read and the
 * write: the configurable rights, the choices of the personal settings, the LDAP defaults and the
 * token and device actions of an administrator.
 */

import { request } from "./client";
import type { PostData, ResponseAction } from "./types";

/** A value and its label: a `UserRightValue` constant of a right (`UserRightInfo.Option`), or a setting (`UserSettingsOptions.Option`). */
export interface LabeledOption {
  value: string;
  label: string;
}

/** A configurable right as `UserRightsHandler.getRightInfos` describes it. */
export interface UserRightInfo {
  rightId: string;
  label: string;
  /** A checkbox (`booleanValue`) rather than a choice of `values` (`value`). */
  booleanType: boolean;
  values: LabeledOption[];
  value?: string | null;
  booleanValue?: boolean | null;
}

/** A right as the user DTO carries it (`UserRightDto`), i.e. what a save posts. */
export interface UserRightValue {
  rightId: string;
  value?: string | null;
  booleanValue?: boolean | null;
}

/** The choices of the personal settings (`UserEntityRest.SettingsOptions`). */
export interface UserSettingsOptions {
  /** Without the "default" entry: no locale (null) is the default. */
  locales: LabeledOption[];
  dateFormats: LabeledOption[];
  excelDateFormats: LabeledOption[];
  timeNotations: LabeledOption[];
  /** The configured samba SID prefix, shown with the samba fields; absent without samba accounts. */
  sambaSIDPrefix?: string | null;
}

export const USER_SETTINGS_OPTIONS_QUERY_KEY = [
  "user",
  "settingsOptions",
] as const;

/**
 * The configurable rights of a user with the given groups — which rights there are and which values
 * they offer depend on the groups, so the form asks again whenever they change.
 *
 * @param userRights The rights as the form holds them: a value not saved yet survives the new answer
 *   unless the right no longer offers it.
 */
export async function fetchUserRights(
  body: {
    userId: number | null;
    assignedGroups: { id: number }[];
    userRights: UserRightValue[];
  },
  signal?: AbortSignal
): Promise<UserRightInfo[]> {
  return request<UserRightInfo[]>(
    "/rs/user/rights",
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
}

export async function fetchUserSettingsOptions(
  signal?: AbortSignal
): Promise<UserSettingsOptions> {
  return request<UserSettingsOptions>(
    "/rs/user/settingsOptions",
    { method: "GET" },
    signal
  );
}

/** The LDAP values of a user as the DTO carries them (`UserLdapValues`). */
export interface UserLdapValuesDto {
  asString?: string | null;
  uidNumber?: number | null;
  gidNumber?: number | null;
  homeDirectory?: string | null;
  loginShell?: string | null;
  sambaPrimaryGroupSIDNumber?: number | null;
  sambaSIDNumber?: number | null;
  sambaNTPassword?: string | null;
}

/**
 * The default posix (`createUid`) or samba (`createSambaSID`) values the backend proposes for the user
 * in the form. The whole user goes with the request, as the endpoints answer it back with its LDAP
 * values completed; only those are read here.
 */
export async function fetchUserLdapDefaults(
  kind: "createUid" | "createSambaSID",
  data: unknown,
  signal?: AbortSignal
): Promise<UserLdapValuesDto | null> {
  const postData: PostData = { data } as PostData;
  const action = await request<ResponseAction>(
    `/rs/user/${kind}`,
    { method: "POST", body: JSON.stringify(postData) },
    signal
  );
  const user = action.variables?.data as
    | { ldapValues?: UserLdapValuesDto }
    | undefined;
  return user?.ldapValues ?? null;
}

export type UserTokenType = "CALENDAR_REST" | "DAV_TOKEN" | "REST_CLIENT";

/** What the token and device actions answer of the user: the refreshed time agos. */
export interface UserAccessInfo {
  calendarExportTokenCreationTimeAgo?: string | null;
  davTokenCreationTimeAgo?: string | null;
  restClientTokenCreationTimeAgo?: string | null;
  stayLoggedInDevices?: number | null;
  stayLoggedInLastAccessTimeAgo?: string | null;
}

/** Renews a token of the user (`UserEntityRest.resetToken`); answers the user's refreshed time agos. */
export async function renewUserToken(
  userId: number,
  type: UserTokenType,
  signal?: AbortSignal
): Promise<UserAccessInfo> {
  const action = await request<ResponseAction>(
    `/rs/user/resetToken?userId=${userId}&type=${type}`,
    { method: "POST", body: JSON.stringify({ data: { id: userId } }) },
    signal
  );
  return (action.variables?.data ?? {}) as UserAccessInfo;
}

/** Logs out every stay-logged-in device of the user (`UserServicesRest.logoutAllDevicesOfUser`). */
export async function logoutAllDevicesOfUser(
  userId: number,
  signal?: AbortSignal
): Promise<UserAccessInfo> {
  const action = await request<ResponseAction>(
    `/rs/user/logoutAllDevicesOfUser?userId=${userId}`,
    { method: "POST", body: JSON.stringify({ data: { id: userId } }) },
    signal
  );
  if (action.validationErrors?.length) {
    throw new Error(action.validationErrors.map((it) => it.message).join(" "));
  }
  return (action.variables?.data ?? {}) as UserAccessInfo;
}
