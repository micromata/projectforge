// Mirrors org.projectforge.rest.dto.LeaveAccountEntry (projectforge-rest). Keep field names in sync with the Spring
// DTO — the employee is a reference ({id, displayName}); dateFormatted / amountFormatted are read-only.

/** The referenced employee as the DTO carries it (org.projectforge.rest.dto.Employee, minimal). */
export interface LeaveAccountEntryEmployee {
  id: number;
  displayName?: string | null;
}

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses `JsonInclude.Include.NON_NULL`
 * (JacksonConfiguration), so an empty field is absent from the JSON rather than null. `toFormValues` normalises
 * that away.
 */
export interface LeaveAccountEntryDetail {
  /** null for an entry that has not been saved yet (Spring assigns the id). */
  id: number | null;
  employee?: LeaveAccountEntryEmployee | null;
  /** ISO date (yyyy-MM-dd). */
  date?: string | null;
  dateFormatted?: string | null;
  /** The correction in days, positive or negative, with at most one decimal place. */
  amount?: number | null;
  description?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface LeaveAccountEntryListRow extends LeaveAccountEntryDetail {
  id: number;
}
