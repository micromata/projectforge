/** Types of the System (administration) page (`/next/system`), successor of Wicket's `wa/admin`. */

/** Initial state of the page, as `SystemRest.SystemAdminData` serializes it. */
export interface SystemAdminData {
  /** The system alert message currently shown site-wide, or null if none is set. */
  alertMessage: string | null;
  /** Default for the reindex "newest N entries" field (1000 in the classic page). */
  reindexNewestNEntries: number;
  /** Whether ProjectForge runs in development mode — gates the developer tools. */
  developmentMode: boolean;
  /** The localized copy&paste maintenance-notice sample, with the next full hour filled in. */
  alertMessageSampleMaintenance: string;
  /** The localized copy&paste new-release-notice sample, with the next full hour and the next release. */
  alertMessageSampleRelease: string;
  /** True if Sipgate is configured: the menu bar offers its actions then. */
  sipgateConfigured: boolean;
}
