import type { GroupValues } from "./group-schema";
import type { GroupDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` — which a
 * controlled input would read as "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(group: GroupDetail): GroupValues {
  return {
    id: group.id ?? null,
    name: group.name ?? null,
    organization: group.organization ?? null,
    description: group.description ?? null,
    localGroup: group.localGroup ?? false,
    groupOwner: group.groupOwner ?? null,
    assignedUsers: group.assignedUsers ?? [],
    gidNumber: group.gidNumber ?? null,
    ldapPosixConfigured: group.ldapPosixConfigured ?? false,
    emails: group.emails ?? null,
    ldapValues: group.ldapValues ?? null,
    created: group.created ?? null,
  };
}

/**
 * Blank form for a group that doesn't exist yet.
 *
 * `ldapPosixConfigured` starts false, so the LDAP field is absent until the backend says otherwise:
 * an "add" reads its preset from `/rs/group/newEntry` (`AbstractDTOPagesRest.newBaseDTO` runs
 * `transformFromDB`, which sets the flag), and the form is reset onto that answer.
 */
export function emptyGroupValues(): GroupValues {
  return {
    id: null,
    name: null,
    organization: null,
    description: null,
    localGroup: false,
    groupOwner: null,
    assignedUsers: [],
    gidNumber: null,
    ldapPosixConfigured: false,
    emails: null,
    ldapValues: null,
    created: null,
  };
}
