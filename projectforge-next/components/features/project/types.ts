// Mirrors org.projectforge.rest.dto.Project (projectforge-rest). Keep field names in sync with the
// Spring DTO. The customer, the account and the cost 2 types have no counterpart in ProjektDO's
// generated metadata (foreign DOs without `UIDataType`, and a DTO-only list), and `kostFormatted`,
// `nummernkreis`, `bereich`, `kost2ArtsAsString` and `statusAsString` are computed read-only values.

import type { PROJEKT_METADATA } from "@/lib/metadata/projekt.generated";

/** The constants of org.projectforge.business.fibu.ProjektStatus, from the metadata. */
export type ProjektStatus =
  (typeof PROJEKT_METADATA.fields.status.enumValues)[number]["value"];

/** A referenced object as the DTO carries it: the id to write back, the name to show. */
export type RefDto = {
  id: number;
  displayName?: string;
};

/** The customer of a project. Its id *is* the customer number (`Customer.copyFrom`). */
export type ProjectCustomerDto = RefDto & {
  name?: string | null;
  division?: string | null;
};

/** The task of a project: the title for the list, the path to the root for its tooltip. */
export type ProjectTaskDto = RefDto & {
  title?: string | null;
  path?: string | null;
};

/**
 * One cost 2 type (org.projectforge.rest.dto.Kost2Art) as the edit form offers it: the ones the project
 * already has a cost 2 unit for are `existsAlready`, those with an active one also `active`. `selected` is
 * the checkbox, starting as `active` — `ProjectEntityRest.onAfterSaveOrUpdate` creates or reactivates a
 * cost 2 unit for each selected one after the save, and sets an unselected active one non-active.
 */
export interface Kost2ArtSelection {
  id: number;
  name?: string | null;
  fakturiert?: boolean;
  projektStandard?: boolean;
  description?: string | null;
  selected: boolean;
  existsAlready: boolean;
  active: boolean;
}

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface ProjectDetail {
  id: number | null;
  /** The local project number — unique within its customer or the range without customers. */
  nummer?: number | null;
  /** The system-wide unique project number, e.g. 512345 (range 5) or a free one. */
  projektnummer?: number | null;
  name?: string | null;
  identifier?: string | null;
  description?: string | null;
  status?: ProjektStatus | null;
  customer?: ProjectCustomerDto | null;
  /** The DATEV account of the project. Written back by id. */
  konto?: RefDto | null;
  task?: ProjectTaskDto | null;
  projektManagerGroup?: RefDto | null;
  contactPerson?: RefDto | null;
  /** The further contact persons next to `contactPerson` (`Project.additionalContacts`). */
  additionalContacts?: RefDto[] | null;
  /** The contact person and the further ones, full names, for the list column only. */
  assignedPersons?: string | null;
  /** Digit 1 of the project's Kost2 (computed from the project number, absent for a free one). Read-only. */
  nummernkreis?: number | null;
  /** Digits 2-4 of the project's Kost2 (storage format of Kost2DO). Read-only. */
  bereich?: number | null;
  /** Legacy: digits 2-4 of an internal project; not used any more (see ProjektDO.internKost2_4). */
  internKost2_4?: number | null;
  /**
   * All cost 2 types for the edit form; in a list row only the project's existing ones, with `active`
   * (struck through when not, see ProjectKost2ArtsCell).
   */
  kost2Arts?: Kost2ArtSelection[] | null;
  /** The formatted project number ("5.123.04", "1.0007.3", or a free one). Read-only. */
  kostFormatted?: string | null;
  /** The two-digit ids of the project's cost 2 types ("00, 01, 03") — list rows only. Read-only. */
  kost2ArtsAsString?: string | null;
  /** Day of the latest time sheet on the project's task tree (ISO date) — list rows only. Read-only. */
  lastTimesheetDate?: string | null;
  /** Youngest date of the project's orders (ISO date) — list rows only. Read-only. */
  lastOrderDate?: string | null;
  /** The later of `lastTimesheetDate` and `lastOrderDate` — list rows only. Read-only. */
  lastActivityDate?: string | null;
  /** The translated status label (`Project.statusAsString`). Read-only. */
  statusAsString?: string | null;
  /**
   * The project has cost 2 units, so its number and customer are fixed (`ProjektDao.isNumberLocked`).
   * Edit form only. Read-only.
   */
  numberLocked?: boolean | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface ProjectListRow extends ProjectDetail {
  id: number;
}
