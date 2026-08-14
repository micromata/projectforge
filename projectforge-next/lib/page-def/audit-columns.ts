/**
 * The two timestamps every entity carries, offered as columns by every list.
 *
 * `created` and `lastUpdate` are properties of `ExtendedBaseDO` (and of `BaseDTO`, which every row is a
 * projection of), so no page has to declare them: appending them here is what makes "when was this
 * entry made, when was it last touched" answerable in *every* list rather than in the three that
 * happened to declare it.
 *
 * They start hidden. A list is read for its own subject, and two timestamp columns at the right edge
 * of every table would push it aside; what the user does with the column panel is then stored per user
 * and entity as usual (see useColumnStatePersistence), so switching one on is a decision that sticks.
 *
 * A page that declares one of them itself keeps its own declaration — its width, its label and, above
 * all, its visibility: an order's `lastUpdate` is a column of the list, not an option of it.
 */

import type { EntityMetadata } from "@/lib/metadata/types";
import { columnIdOf } from "./define-page";
import type { ColumnDeclaration, FieldColumn } from "./types";

/** In the order they read: made, then changed. */
export const AUDIT_COLUMN_NAMES = ["created", "lastUpdate"] as const;

/** Wide enough for a date and a time in every locale — the width the pages that declare it use. */
const AUDIT_COLUMN_SIZE = 130;

/**
 * The audit columns a page's declarations don't already hold, ready to be appended to them.
 *
 * Skips what the entity has no field for: the metadata is the generator's, and a DTO the backend fills
 * without a timestamp would otherwise get a column that is empty for every row.
 */
export function auditColumnsFor<Row, M extends EntityMetadata>(
  columns: ColumnDeclaration<Row, M>[],
  metadata: EntityMetadata
): FieldColumn<Row, M>[] {
  const declared = new Set(columns.map((column) => columnIdOf(column)));
  return AUDIT_COLUMN_NAMES.filter(
    (name) => !declared.has(name) && metadata.fields[name]
  ).map((name) => ({ name, size: AUDIT_COLUMN_SIZE }) as FieldColumn<Row, M>);
}

/**
 * The visibility a list starts with: the appended audit columns off, everything else as declared.
 *
 * Only the appended ones — a column the page declares is shown because it declared it (see the file
 * comment). The user's own visibility is merged over this (see useTableState), and a reset returns to
 * it rather than to "everything visible".
 */
export function defaultVisibilityOf<Row, M extends EntityMetadata>(
  appended: FieldColumn<Row, M>[]
): Record<string, boolean> {
  return Object.fromEntries(appended.map((column) => [column.name, false]));
}
