// Mirrors org.projectforge.rest.dto.Kost1 (projectforge-rest). Keep field names in sync with the
// Spring DTO — the number is four fields there, not one string, which is what the edit form binds to.

import type { KOST1_METADATA } from "@/lib/metadata/kost1.generated";

/** The constants of org.projectforge.business.fibu.kost.KostentraegerStatus, from the metadata. */
export type KostentraegerStatus =
  (typeof KOST1_METADATA.fields.kostentraegerStatus.enumValues)[number]["value"];

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface Cost1Detail {
  /** null for a cost unit that has not been saved yet (Spring assigns the id). */
  id: number | null;
  nummernkreis: number;
  bereich: number;
  teilbereich: number;
  endziffer: number;
  kostentraegerStatus?: KostentraegerStatus | null;
  description?: string | null;
  /**
   * `#.###.##.##`, computed by the entity (Kost1DO.formattedNumber has no backing field). Read-only:
   * the list shows it, the form never sends one back.
   */
  formattedNumber?: string | null;
  created?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface Cost1ListRow extends Cost1Detail {
  id: number;
}
