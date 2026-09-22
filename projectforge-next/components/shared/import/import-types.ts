/**
 * The wire shape and the config contract of the generic, hand-built CSV/DATEV import flow
 * (`AbstractImportRest` / `ImportView` on the backend).
 *
 * These mirror the Kotlin exactly. Spring serialises with `JsonInclude.NON_NULL`, so a null field is
 * absent from the JSON — every optional field is typed accordingly. The types are entity-agnostic: the
 * parsed row (`ImportEntry.read`) is an opaque record addressed by dotted path, so one preview table,
 * one diff cell and one hook serve every import that supplies an {@link ImportConfig}.
 */

/** `ImportEntry.Status` — the reconciliation state of one row. `statusAsString` carries its localised text. */
export type ImportStatus =
  | "NEW"
  | "DELETED"
  | "MODIFIED"
  | "UNMODIFIED"
  | "IMPORTED"
  | "UNKNOWN_MODIFICATION"
  | "UNKNOWN"
  | "FAULTY";

/** `ImportStorageInfo.ColumnMapping` — one detected column: the target field's label and the matched CSV header. */
export interface ImportColumnMapping {
  field?: string;
  header?: string;
}

/** `ImportStorageInfo` — the aggregated counts and the detected/unknown columns of an upload. */
export interface ImportStorageInfo {
  totalNumber: number;
  numberOfNewEntries: number;
  numberOfDeletedEntries: number;
  numberOfModifiedEntries: number;
  numberOfUnmodifiedEntries: number;
  numberOfUnknownEntries: number;
  numberOfFaultyEntries: number;
  detectedColumns?: string[];
  /** The detected columns as field-label→matched-CSV-header pairs, in the file's column order. */
  detectedColumnMappings?: ImportColumnMapping[];
  unknownColumns?: string[];
}

/** The parsed row, addressed by dotted path relative to `read` (e.g. `konto.nummer`). */
export type ImportRead = Record<string, unknown>;

/** `ImportEntry` — one line of the file, with its reconciliation state and (for MODIFIED) the old values. */
export interface ImportEntry<Read extends ImportRead = ImportRead> {
  id: number;
  status: ImportStatus;
  /** Already localised by the backend — displayed as it is. */
  statusAsString: string;
  /** The parsed/current DTO row. */
  read?: Read;
  error?: string;
  hasError: boolean;
  /**
   * Only present on MODIFIED entries: the old (stored) value of each changed property, keyed by the
   * `read.`-prefixed property path (e.g. `read.konto.nummer`). The current value is `read.<path>`.
   */
  oldDiffValues?: Record<string, unknown> | null;
}

/** `ImportView` — the whole import in progress. An empty view (no `filename`/`info`) means "nothing stashed". */
export interface ImportView<Read extends ImportRead = ImportRead> {
  filename?: string;
  title?: string;
  hasBeenReconciled: boolean;
  info?: ImportStorageInfo;
  entries: ImportEntry<Read>[];
  meta?: Record<string, unknown>;
}

/** `ImportStorage.DisplayOptions` — which statuses the reconcile answer includes. All optional/nullable. */
export interface DisplayOptions {
  new?: boolean | null;
  modified?: boolean | null;
  unmodified?: boolean | null;
  imported?: boolean | null;
  deleted?: boolean | null;
  faulty?: boolean | null;
  unknown?: boolean | null;
}

/** How a column formats its value (see `formatByKind` in import-model.ts). */
export type ImportColumnKind =
  | "text"
  | "date"
  | "currency"
  | "percentage"
  | "number";

/** One column of the preview table, entity-agnostic. */
export interface ImportColumn {
  /** Accessor path relative to `entry.read`, e.g. `konto.nummer`. Also the diff key stem (`read.` + field). */
  field: string;
  /** i18n key of the header (an existing backend bundle key). */
  headerKey: string;
  kind: ImportColumnKind;
  /** Renders the old→new diff for MODIFIED rows via `oldDiffValues["read." + field]`. */
  diff?: boolean;
  width?: number;
  /** Gate the column on the view's `meta`, e.g. `m => m.isPositionBasedImport === true`. */
  showIf?: (meta: Record<string, unknown>) => boolean;
}

/** Everything a concrete import supplies; the generic flow needs nothing else. */
export interface ImportConfig {
  /** REST path base under `/rs`, e.g. `incomingInvoiceImport`. */
  endpoints: { base: string };
  /** i18n key of the page/upload title. */
  titleKey: string;
  columns: ImportColumn[];
  /** `<input accept>` value for the drop step, e.g. `.csv`. */
  fileAccept: string;
  /** Where committing returns to, e.g. `/creditor-invoice`. */
  returnRoute: string;
  /** Which statuses the user may tick; defaults to the importable ones (NEW, MODIFIED, DELETED). */
  selectableStatuses?: ImportStatus[];
}
