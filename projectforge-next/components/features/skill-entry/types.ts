// Mirrors org.projectforge.rest.dto.SkillEntry (skillmatrix plugin). Keep field names in sync with the
// Spring DTO.

/**
 * The owner as the DTO carries it: the id to write back, the name to show. A type alias rather than an
 * interface, so it satisfies the index signature of the schema's `looseObject`.
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
export interface SkillEntryDetail {
  /** null for a skill that has not been saved yet (Spring assigns the id). */
  id: number | null;
  skill?: string | null;
  /** The logged-in user for a new skill and a clone (`SkillEntryEntityRest.newBaseDO`/`prepareClone`). */
  owner?: UserRefDto | null;
  /** 0 (not rated) .. 3 (`SkillEntryDO.MAX_VAL_RATING`). */
  rating?: number | null;
  /** 0 (not rated) .. 3 (`SkillEntryDO.MAX_VAL_INTEREST`). */
  interest?: number | null;
  comment?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface SkillEntryListRow extends SkillEntryDetail {
  id: number;
}
