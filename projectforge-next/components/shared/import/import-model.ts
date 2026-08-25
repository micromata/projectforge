/**
 * The pure logic of the import flow — everything that has no DOM and can be unit-tested on its own
 * (see import-model.test.ts): the row colour and selectability rules, the visible-column filter, the
 * old→new diff extraction and the by-kind formatting. The cells and the table below only render what
 * these return.
 */

import { getByPath, type DataObject } from "@/lib/dynamic/path";
import {
  formatCurrency,
  formatDate,
  formatDisplayName,
  formatNumber,
  formatPercentage,
  type FormatContext,
} from "@/lib/format";
import type {
  ImportColumn,
  ImportColumnKind,
  ImportEntry,
  ImportRead,
  ImportStatus,
  ImportStorageInfo,
} from "./import-types";

/**
 * The row tint per status, mirroring `AbstractImportPageRest.withGetRowClass`: a new row reads green, a
 * changed one blue, and a deleted/faulty/unknown one red. Everything else (unmodified, imported) is plain.
 */
export function rowClassForStatus(status: ImportStatus): string | undefined {
  switch (status) {
    case "NEW":
      return "row-green";
    case "MODIFIED":
      return "row-blue";
    case "DELETED":
    case "FAULTY":
    case "UNKNOWN":
    case "UNKNOWN_MODIFICATION":
      return "row-red";
    default:
      return undefined;
  }
}

/** The statuses the backend actually imports (`AbstractImportRest.commit` keeps only these). */
export const IMPORTABLE_STATUSES: ImportStatus[] = [
  "NEW",
  "MODIFIED",
  "DELETED",
];

/** Whether a row may be ticked — the config's `selectableStatuses`, or the importable ones by default. */
export function isSelectable(
  status: ImportStatus,
  selectableStatuses?: ImportStatus[]
): boolean {
  return (selectableStatuses ?? IMPORTABLE_STATUSES).includes(status);
}

/** The columns to render for the given view metadata: those without a gate, or whose gate passes. */
export function visibleColumns(
  columns: ImportColumn[],
  meta: Record<string, unknown>
): ImportColumn[] {
  return columns.filter((column) => !column.showIf || column.showIf(meta));
}

/** The current and old value of a diff column, and whether they differ (i.e. an old value was sent). */
export interface DiffValues {
  current: unknown;
  old: unknown;
  hasDiff: boolean;
}

/**
 * Reads the current value from `entry.read.<field>` and the old value from
 * `entry.oldDiffValues["read." + field]`. The old value is only present on a MODIFIED row whose property
 * changed, so its presence is what tells a diff from an unchanged cell.
 */
export function diffOf(entry: ImportEntry, column: ImportColumn): DiffValues {
  const current = getByPath(entry.read as DataObject | undefined, column.field);
  const diffKey = `read.${column.field}`;
  const old = entry.oldDiffValues?.[diffKey];
  const hasDiff =
    entry.oldDiffValues != null &&
    Object.prototype.hasOwnProperty.call(entry.oldDiffValues, diffKey);
  return { current, old, hasDiff };
}

/** Formats a cell value the way its column kind asks for, through the user's locale (see lib/format). */
export function formatByKind(
  value: unknown,
  kind: ImportColumnKind,
  ctx: FormatContext
): string {
  switch (kind) {
    case "date":
      return formatDate(value, ctx);
    case "currency":
      return formatCurrency(value, ctx);
    case "percentage":
      return formatPercentage(value, ctx);
    case "number":
      return formatNumber(value, ctx);
    default:
      return formatDisplayName(value);
  }
}

/** One entry of the statistics line: a per-status count, its label key and the tint it reads in. */
export interface ImportStatEntry {
  key: string;
  labelKey: string;
  count: number;
  tone?: "new" | "modified" | "deleted" | "faulty" | "unknown";
}

/**
 * The statistics line's entries, built from the aggregated counts. The total is always shown; a per-status
 * count only when it is non-zero, so a clean import does not carry six zeroes.
 */
export function statisticsEntries(
  info: ImportStorageInfo | undefined
): ImportStatEntry[] {
  if (!info) return [];
  const entries: ImportStatEntry[] = [
    { key: "total", labelKey: "import.stats.total", count: info.totalNumber },
  ];
  const push = (
    count: number,
    key: string,
    labelKey: string,
    tone: ImportStatEntry["tone"]
  ) => {
    if (count > 0) entries.push({ key, labelKey, count, tone });
  };
  push(info.numberOfNewEntries, "new", "import.entry.status.new", "new");
  push(
    info.numberOfModifiedEntries,
    "modified",
    "import.entry.status.modified",
    "modified"
  );
  push(
    info.numberOfDeletedEntries,
    "deleted",
    "import.entry.status.deleted",
    "deleted"
  );
  push(
    info.numberOfUnmodifiedEntries,
    "unmodified",
    "import.entry.status.unmodified",
    undefined
  );
  push(
    info.numberOfUnknownEntries,
    "unknown",
    "import.entry.status.unknown",
    "unknown"
  );
  push(
    info.numberOfFaultyEntries,
    "faulty",
    "import.entry.status.faulty",
    "faulty"
  );
  return entries;
}

/** The ids of the entries a user may still tick, out of a full view — used to gate "select all". */
export function selectableIds(
  entries: ImportEntry<ImportRead>[],
  selectableStatuses?: ImportStatus[]
): number[] {
  return entries
    .filter((entry) => isSelectable(entry.status, selectableStatuses))
    .map((entry) => entry.id);
}
