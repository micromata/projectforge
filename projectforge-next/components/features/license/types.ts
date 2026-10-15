// Mirrors org.projectforge.plugins.licensemanagement.dto.License (licensemanagement plugin). Keep field
// names in sync with the Spring DTO — `owners` (resolved from `LicenseDO.ownerIds`) and `keyVisible` have
// no counterpart in the generated metadata.

/**
 * A referenced user as the DTO carries it: the id to write back (`License.copyTo` stores the ids), the
 * name to show. A type alias rather than an interface, so it satisfies the index signature of the
 * schema's `looseObject`.
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
export interface LicenseDetail {
  /** null for a license that has not been saved yet (Spring assigns the id). */
  id: number | null;
  organization?: string | null;
  product?: string | null;
  version?: string | null;
  updateFromVersion?: string | null;
  licenseHolder?: string | null;
  /** Absent where the user may not see it — see [keyVisible]. */
  key?: string | null;
  numberOfLicenses?: number | null;
  owners?: UserRefDto[] | null;
  device?: string | null;
  comment?: string | null;
  /** `LicenseStatus` (OVERRATED, UPTODATE). Not on the form, only carried through a save. */
  status?: string | null;
  validSince?: string | null;
  validUntil?: string | null;
  /** Names of the two stored files, present only where a file is stored and the key is visible. */
  filename1?: string | null;
  filename2?: string | null;
  /**
   * Whether the user may see the key and the files: an administrator, an owner, or anybody on a new
   * license (`LicenseManagementRight.isLicenseKeyVisible`). Read-only, the backend ignores it on save.
   */
  keyVisible?: boolean;
  /** How many of the two file slots hold a file (1 or 2); absent for none. Read-only. */
  numberOfFiles?: number | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface LicenseListRow extends LicenseDetail {
  id: number;
}
