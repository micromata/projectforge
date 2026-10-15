// Mirrors org.projectforge.rest.dto.Kost2Art (projectforge-rest). Keep field names in sync with the
// Spring DTO.

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface Kost2ArtDetail {
  /**
   * The two-digit cost-2 type number and the primary key alike — entered by the user when the type is
   * created (`Kost2ArtDao.avoidNullIdCheckBeforeSave`), null only while the add form is still empty.
   */
  id: number | null;
  name?: string | null;
  fakturiert?: boolean;
  workFraction?: number | null;
  projektStandard?: boolean;
  description?: string | null;
  /**
   * The id as the two-digit string it is read as ("05"), computed by the DTO (`Kost2Art.getFormattedId`).
   * Read-only: the list shows it, the form binds to the numeric `id`.
   */
  formattedId?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface Kost2ArtListRow extends Kost2ArtDetail {
  id: number;
}
