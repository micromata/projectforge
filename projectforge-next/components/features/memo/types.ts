// Mirrors org.projectforge.plugins.memo.MemoDO (memo plugin), which MemoEntityRest transfers as it is (no
// DTO). Keep field names in sync with the data object.

/**
 * The owner as `MemoDO` serializes it (`IdOnlySerializer`): the id only. A type alias rather than an
 * interface, so it satisfies the index signature of the schema's `looseObject`.
 */
export type MemoOwnerDto = {
  id: number;
};

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface MemoDetail {
  /** null for a memo that has not been saved yet (Spring assigns the id). */
  id: number | null;
  subject?: string | null;
  memo?: string | null;
  /** Always the logged-in user (`MemoEntityRest.newBaseDO`, `MemoDao.onInsertOrModify`). */
  owner?: MemoOwnerDto | null;
  created?: string | null;
  lastUpdate?: string | null;
  deleted?: boolean;
}

/** Projection the list page renders — the same data object, with the id the table keys rows by. */
export interface MemoListRow extends MemoDetail {
  id: number;
}
