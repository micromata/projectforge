import type { UserValues } from "./user-schema";
import type { UserDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` — which a
 * controlled input would read as "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(user: UserDetail): UserValues {
  const ldap = user.ldapValues ?? {};
  return {
    id: user.id ?? null,
    username: user.username ?? "",
    firstname: user.firstname ?? null,
    nickname: user.nickname ?? null,
    lastname: user.lastname ?? null,
    gender: user.gender ?? null,
    organization: user.organization ?? null,
    email: user.email ?? null,
    alternativeEmails: user.alternativeEmails ?? null,
    formerEmails: user.formerEmails ?? null,
    mobilePhone: user.mobilePhone ?? null,
    jiraUsername: user.jiraUsername ?? null,
    description: user.description ?? null,
    hrPlanning: user.hrPlanning ?? false,
    deactivated: user.deactivated ?? false,
    restrictedUser: user.restrictedUser ?? false,
    localUser: user.localUser ?? false,
    timeZone: user.timeZone ?? null,
    locale: user.locale ?? null,
    dateFormat: user.dateFormat ?? null,
    excelDateFormat: user.excelDateFormat ?? null,
    timeNotation: user.timeNotation ?? null,
    personalPhoneIdentifiers: user.personalPhoneIdentifiers ?? null,
    sshPublicKey: user.sshPublicKey ?? null,
    gpgPublicKey: user.gpgPublicKey ?? null,
    assignedGroups: user.assignedGroups ?? [],
    userRights: (user.userRights ?? []).map((right) => ({
      rightId: right.rightId,
      value: right.value ?? null,
      booleanValue: right.booleanValue ?? null,
    })),
    ldapValues: {
      uidNumber: ldap.uidNumber ?? null,
      gidNumber: ldap.gidNumber ?? null,
      homeDirectory: ldap.homeDirectory ?? null,
      loginShell: ldap.loginShell ?? null,
      sambaSIDNumber: ldap.sambaSIDNumber ?? null,
      sambaPrimaryGroupSIDNumber: ldap.sambaPrimaryGroupSIDNumber ?? null,
      sambaNTPassword: ldap.sambaNTPassword ?? null,
    },
    password: null,
    wlanPassword: null,
    lastLoginFormatted: user.lastLoginFormatted ?? null,
    lastPasswordChangeFormatted: user.lastPasswordChangeFormatted ?? null,
    lastWlanPasswordChangeFormatted:
      user.lastWlanPasswordChangeFormatted ?? null,
    posixConfigured: user.posixConfigured ?? false,
    sambaConfigured: user.sambaConfigured ?? false,
    externalUserManagementSystem: user.externalUserManagementSystem ?? false,
    wlanPasswordSupported: user.wlanPasswordSupported ?? false,
    phoneIdentifiersSupported: user.phoneIdentifiersSupported ?? false,
    created: user.created ?? null,
  };
}

/**
 * Blank form for a user that doesn't exist yet. The flags start false, so the LDAP and WLAN fields are
 * absent until the backend says otherwise: an "add" reads its preset from `/rs/user/newEntry`
 * (`UserEntityRest.newBaseDTO`, which also presets the local user flag and the time zone), and the form
 * is reset onto that answer.
 */
export function emptyUserValues(): UserValues {
  return toFormValues({ id: null });
}
