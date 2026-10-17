// Mirrors org.projectforge.rest.dto.Availability. Keep field names in sync with the Spring DTO — the translated
// values, `userEditable`, `internalCommentVisible` and `availableTypes` have no counterpart in the generated
// metadata.
import type {
  ChartDay,
  PersonRef,
  SubstituteAvailability,
} from "@/components/shared/absence-chart/absence-chart-model";

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
export interface AvailabilityDetail extends AvailabilityFormStats {
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
  /** Optional: who stands in for the user during the period. */
  substitutes?: UserRef[] | null;
  substitutesAsString?: string | null;
  deleted?: boolean;
  writeAccess?: boolean;
  deleteAccess?: boolean;
  created?: string | null;
  lastUpdate?: string | null;
}

/**
 * What the form shows of the substitutes' absences and the backend computes for the state being edited
 * (`AvailabilityEntityRest.AvailabilityFormStats`, `/rs/availability/stats`). The loaded entry carries the same
 * fields.
 */
export interface AvailabilityFormStats {
  /** On a day of the period all substitutes are away (ConflictingAvailabilitiesCache). */
  conflict?: boolean | null;
  /** The substitutes' vacations overlapping the period, as the user may see them. */
  substituteVacations?: SubstituteVacation[] | null;
  /** Their availability entries overlapping the period; the absent ones count as vacations. */
  substituteAvailabilities?: SubstituteAvailability[] | null;
  /** The axis of the chart: the period with three days on either side, the uncovered days marked. */
  chartDays?: ChartDay[] | null;
  /** Client-only: the request for the form's current values failed (useAvailabilityStats). */
  unavailable?: boolean;
}

/** A vacation of a substitute (`org.projectforge.rest.dto.Vacation`, as far as the form shows it). */
export interface SubstituteVacation {
  id?: number | null;
  /** With its user: the chart's rows are the substitutes, users. */
  employee?: (PersonRef & { user?: PersonRef | null }) | null;
  startDate?: string | null;
  endDate?: string | null;
  startDateFormatted?: string | null;
  endDateFormatted?: string | null;
  halfDayBegin?: boolean | null;
  halfDayEnd?: boolean | null;
  status?: string | null;
  statusString?: string | null;
  workingDaysFormatted?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface AvailabilityListRow extends AvailabilityDetail {
  id: number;
}
