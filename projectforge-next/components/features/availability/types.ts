// Mirrors org.projectforge.rest.dto.Availability. Keep field names in sync with the Spring DTO — the translated
// values, `userEditable`, `internalCommentVisible` and `availableTypes` have no counterpart in the generated
// metadata.

import type { AVAILABILITY_METADATA } from "@/lib/metadata/availability.generated";

/** The constants of org.projectforge.business.availability.AvailabilityStatus, from the metadata. */
export type AvailabilityStatus =
  (typeof AVAILABILITY_METADATA.fields.status.enumValues)[number]["value"];

/** The constants of org.projectforge.business.availability.AvailabilityLocation, from the metadata. */
export type AvailabilityLocation =
  (typeof AVAILABILITY_METADATA.fields.location.enumValues)[number]["value"];

/** A referenced user as the DTO carries it: the id to write back, the name to show. */
export type UserRef = {
  id: number;
  displayName?: string;
};

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper leaves empty fields out of the JSON
 * (`JsonInclude.Include.NON_NULL`). `toFormValues` normalises that away.
 */
export interface AvailabilityDetail {
  /** null for an entry that has not been saved yet (Spring assigns the id). */
  id: number | null;
  /** Of a user, not of an employee: freelancers without an employee record enter their availability, too. */
  user?: UserRef | null;
  startDate?: string | null;
  endDate?: string | null;
  type?: string | null;
  status?: AvailabilityStatus | null;
  location?: AvailabilityLocation | null;
  /** Visible to everybody who may see the entry. */
  comment?: string | null;
  /** Only sent to the user and HR staff (AvailabilityDao.afterLoad). */
  internalComment?: string | null;
  /** Whether the logged-in user may choose the user (HR staff only). Read-only, edit view only. */
  userEditable?: boolean;
  /** Whether the internal comment is offered (the user and HR staff). Read-only, edit view only. */
  internalCommentVisible?: boolean;
  /** The configured types and the stored one. Read-only, edit view only. */
  availableTypes?: string[] | null;
  deleted?: boolean;
  writeAccess?: boolean;
  deleteAccess?: boolean;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface AvailabilityListRow extends AvailabilityDetail {
  id: number;
}
