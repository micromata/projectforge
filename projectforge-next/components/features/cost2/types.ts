// Mirrors org.projectforge.rest.dto.Kost2 (projectforge-rest). Keep field names in sync with the
// Spring DTO — the number is four fields there (the last one the Kost2Art's id), not one string,
// which is what the edit form binds to.

import type { KOST2_METADATA } from "@/lib/metadata/kost2.generated";

/** The constants of org.projectforge.business.fibu.kost.KostentraegerStatus, from the metadata. */
export type KostentraegerStatus =
  (typeof KOST2_METADATA.fields.kostentraegerStatus.enumValues)[number]["value"];

/** The referenced project as the DTO carries it (org.projectforge.rest.dto.Project, minimal). */
export interface Cost2Project {
  id: number;
  name?: string | null;
  customer?: { id?: number; name?: string | null } | null;
}

/** The referenced cost type as the DTO carries it (org.projectforge.rest.dto.Kost2Art). */
export interface Cost2Art {
  id: number;
  name?: string | null;
  fakturiert?: boolean;
}

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface Cost2Detail {
  /** null for a cost unit that has not been saved yet (Spring assigns the id). */
  id: number | null;
  nummernkreis: number;
  bereich: number;
  teilbereich: number;
  /** The Kost2Art's id (0-99), the number's last part — mirrors `Kost2.endziffer`. */
  endziffer: number;
  kostentraegerStatus?: KostentraegerStatus | null;
  /** Read-only: the status the entity computes, inherited from the project when the row has none. */
  effectiveKostentraegerStatus?: KostentraegerStatus | null;
  workFraction?: number | null;
  description?: string | null;
  comment?: string | null;
  /**
   * `#.###.##.##`, computed by the entity (Kost2DO.formattedNumber has no backing field). Read-only:
   * the list shows it, the form never sends one back.
   */
  formattedNumber?: string | null;
  project?: Cost2Project | null;
  kost2Art?: Cost2Art | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface Cost2ListRow extends Cost2Detail {
  id: number;
}
