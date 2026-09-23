// Mirrors org.projectforge.rest.dto.Customer (projectforge-rest). Keep field names in sync with the
// Spring DTO. Two of its values have no counterpart in KundeDO's generated metadata: `konto` (a
// foreign DO — `KontoDO` has no `UIDataType`, so `ElementsRegistry` never reports it) and the
// computed read-only `kost`/`statusAsString`.

import type { KUNDE_METADATA } from "@/lib/metadata/kunde.generated";

/** The constants of org.projectforge.business.fibu.KundeStatus, from the metadata. */
export type KundeStatus =
  (typeof KUNDE_METADATA.fields.status.enumValues)[number]["value"];

/**
 * A referenced account as the DTO carries it: the id to write back (`BaseDTO.copyTo` resolves the
 * `KontoDO` by it), the name to show. Same shape the invoice's `konto` binds to.
 */
export type KontoRefDto = {
  id: number;
  displayName?: string;
};

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface CustomerDetail {
  /**
   * The customer number *is* the id — `Customer.copyFrom` sets `id = src.nummer` (KundeDO uses
   * `nummer` as its `@Id`). null for a customer that has not been saved yet.
   */
  id: number | null;
  /** The user-assigned customer number, 0..999 (`KundeDO.MAX_ID`); equals [id] once saved. */
  nummer?: number | null;
  name?: string | null;
  identifier?: string | null;
  division?: string | null;
  status?: KundeStatus | null;
  description?: string | null;
  /** The DATEV account of the customer. Written back by id (see KontoRefDto). */
  konto?: KontoRefDto | null;
  /**
   * The formatted number ("5.###"), computed by the entity (`KundeDO.kost` has no backing field).
   * Read-only: the list shows it, the form never sends one back.
   */
  kost?: string | null;
  /** The translated status label, computed on the DTO (`Customer.statusAsString`). Read-only. */
  statusAsString?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface CustomerListRow extends CustomerDetail {
  id: number;
}
