// Mirrors org.projectforge.rest.dto.Group (projectforge-rest). Keep field names in sync with the
// Spring DTO — three of its fields have no counterpart in GroupDO and therefore none in the
// generated metadata either: `assignedUsers` (a collection), `gidNumber` (LDAP) and the computed
// `emails`.

/**
 * A referenced user as the DTO carries it: the id to write back (`BaseDTO.copyTo` resolves the
 * `PFUserDO` by it), the name to show.
 *
 * A type alias rather than an interface, so it satisfies the index signature of the schema's
 * `looseObject` — see `EntityRefDto` in the order feature, which is the same shape for the same
 * reason. Not shared with it because features don't import from each other.
 */
export type UserRefDto = {
  id: number;
  displayName?: string;
};

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface GroupDetail {
  /** null for a group that has not been saved yet (Spring assigns the id). */
  id: number | null;
  name?: string | null;
  organization?: string | null;
  description?: string | null;
  /** A group that exists in ProjectForge only, i.e. is not exported to LDAP. */
  localGroup?: boolean | null;
  groupOwner?: UserRefDto | null;
  /** The members, sorted by display name (`Group.copyFrom`). */
  assignedUsers?: UserRefDto[] | null;
  /** LDAP gid, only meaningful where [ldapPosixConfigured] holds. */
  gidNumber?: number | null;
  /**
   * Whether posix accounts are in use at all, i.e. whether the LDAP field belongs on the form.
   * Read-only: the backend decides it per request (`GroupPagesRest.transformFromDB`), it is no
   * property of the group.
   */
  ldapPosixConfigured?: boolean | null;
  /**
   * The mail addresses of every member, comma separated — computed on read
   * (`Group.populateEmails`), so read-only here as it is there.
   */
  emails?: string | null;
  /** The LDAP attributes as they are stored, shown in the list for administrators. */
  ldapValues?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface GroupListRow extends GroupDetail {
  id: number;
}
