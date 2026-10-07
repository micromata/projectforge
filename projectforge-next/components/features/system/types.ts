/** Types of the System (administration) page (`/next/system`), successor of Wicket's `wa/admin`. */

/** Initial state of the page, as `SystemRest.SystemAdminData` serializes it. */
export interface SystemAdminData {
  /** The system alert message currently shown site-wide, or null if none is set. */
  alertMessage: string | null;
  /** Default for the reindex "newest N entries" field (1000 in the classic page). */
  reindexNewestNEntries: number;
  /** Whether ProjectForge runs in development mode — gates the developer tools. */
  developmentMode: boolean;
  /** The localized copy&paste maintenance-notice sample, with the current version filled in. */
  alertMessageSample: string;
  /** True if all files are stored by the file store (`projectforge.files.store=db`), false: JCR. */
  allFilesInFileStore: boolean;
}
