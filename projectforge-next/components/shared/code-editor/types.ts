/** A former version of some code, as a [CodeVersionSource] lists it. */
export interface CodeVersion {
  /** Names the version for [CodeVersionSource.load]. */
  key: string;
  /** When it was replaced, in epoch millis; null where that is not known. */
  timestamp: number | null;
  /** How the version pickers name it, instead of "backup of …" (a history entry, say). */
  label?: string;
}

/**
 * Where the former versions of an editor's code come from — the backups of a script, say — so that the
 * diff can compare them too (CodeDiffDialog). Fetched only when the diff is opened.
 */
export interface CodeVersionSource {
  /** The query key of the versions; each version's code is cached under it as well. */
  queryKey: readonly unknown[];
  /** The versions, newest first. */
  list: (signal?: AbortSignal) => Promise<CodeVersion[]>;
  load: (key: string, signal?: AbortSignal) => Promise<string>;
  /** Told where there are none, instead of "there are no backups yet". */
  emptyText?: string;
}
