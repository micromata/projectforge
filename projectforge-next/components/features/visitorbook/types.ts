// Mirrors org.projectforge.rest.dto.Visitorbook and VisitorbookEntry (projectforge-rest). Keep field names in sync
// with the Spring DTOs — lastDateOfVisit, latestArrived, latestDeparted and numberOfVisits are read-only, taken
// from the latest visit; entries come with the form only.

import type { ValidityEntry } from "@/components/shared/validity-entries/types";

/** One visit of a visitor: the day, the times of arrival and departure (free text, `HH:mm`) and a comment. */
export interface VisitorbookEntry extends ValidityEntry {
  dateOfVisit?: string | null;
  arrived?: string | null;
  departed?: string | null;
  visitorbookId?: number | null;
}

/** A contact person, as `Visitorbook.contactPersons` carries it: the id is what `copyTo` stores. */
export type VisitorbookContactPerson = {
  id: number;
  displayName?: string | null;
};

export type VisitorType =
  | "NORMAL"
  | "EXTENDED"
  | "FAMILY"
  | "SUPPLIER"
  | "WORKMAN";

export interface VisitorbookDetail {
  /** null for a visitor that has not been saved yet (Spring assigns the id). */
  id: number | null;
  firstname?: string | null;
  lastname?: string | null;
  company?: string | null;
  visitortype?: VisitorType | null;
  contactPersons?: VisitorbookContactPerson[] | null;
  /** Read-only: the contact persons' names, joined. */
  contactPersonsAsString?: string | null;
  comment?: string | null;
  /** Read-only, of the latest visit. */
  lastDateOfVisit?: string | null;
  /** Read-only, of the latest visit. */
  latestArrived?: string | null;
  /** Read-only, of the latest visit. */
  latestDeparted?: string | null;
  /** Read-only. */
  numberOfVisits?: number | null;
  /** Only in the form (`transformFromDB` in edit mode), newest first. */
  entries?: VisitorbookEntry[] | null;
  writeAccess?: boolean | null;
  deleteAccess?: boolean | null;
  created?: string | null;
  lastUpdate?: string | null;
}

export interface VisitorbookListRow extends VisitorbookDetail {
  id: number;
}
