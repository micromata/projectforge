/**
 * The two rules that turn a field of the generated metadata into a column or a form field, plus the
 * identity function that binds a page declaration to its entity's field names.
 *
 * They are kept apart from the renderers so they can be tested without a DOM (see vitest.config.mts):
 * what is worth asserting is the derivation, not the JSX around it.
 */

import type { FilterKind } from "@/components/data-table";
import { leafKeyOf } from "@/lib/leaf-key";
import type { EntityMetadata, UIDataTypeName } from "@/lib/metadata/types";
import type { EntityWithId } from "@/hooks/use-entity-detail";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { ColumnDeclaration, EditablePageDef, PageDef } from "./types";

/**
 * Binds a declaration to one entity: every `name` is checked against `keyof metadata.fields`, so a
 * field renamed in the entity fails the typecheck instead of silently rendering an empty column.
 *
 * Nothing else happens here — the declaration is the value, and both renderers read it as it is.
 *
 * For a page with a form. A page whose list is migrated and whose form is not yet uses
 * [defineListPage]: `PageDef.edit` is optional, so a single function would hand `EntityEditPage` a
 * declaration that may have no form, and the check would move from the typechecker into the renderer.
 */
export function definePage<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>(
  def: EditablePageDef<Row, Values, Data, M>
): EditablePageDef<Row, Values, Data, M> {
  return def;
}

/**
 * A list whose entries are still edited in the legacy page — the same declaration without the form
 * half (see PageDef.edit and useEditTargets).
 *
 * It names no `Values`/`Data`, because those describe a form: what such a page has is rows, and the
 * row type is all its columns, `rowClassName` and `statistics` ever read.
 */
export function defineListPage<Row extends ListRow, M extends EntityMetadata>(
  def: Omit<PageDef<Row, never, EntityWithId, M>, "edit">
): PageDef<Row, never, EntityWithId, M> {
  return def;
}

/**
 * The table's column id of a declaration — which is also what the backend sorts by: a field's name, a
 * computed column's explicit id, and a period's begin (the property of the pair the query orders on).
 */
export function columnIdOf<Row, M extends EntityMetadata>(
  declaration: ColumnDeclaration<Row, M>
): string {
  if ("periodLabelKey" in declaration) return declaration.begin;
  return "name" in declaration ? declaration.name : declaration.id;
}

/**
 * The message key a column's header shows: the short label where one is declared, otherwise the
 * column's own (a computed column's `labelKey`, a period's `periodLabelKey`), otherwise the `i18nKey`
 * the field carries in the entity — and the column's id where the metadata knows none.
 *
 * Not the same as [labelKeyFor], which resolves the `<key>._` of a key that is both leaf and
 * namespace: that needs a translator, and this is the part that doesn't.
 */
export function columnHeaderKeyOf<Row, M extends EntityMetadata>(
  declaration: ColumnDeclaration<Row, M>,
  metadata: EntityMetadata
): string {
  const id = columnIdOf(declaration);
  const own =
    "periodLabelKey" in declaration
      ? declaration.periodLabelKey
      : declaration.labelKey;
  return (
    declaration.headerLabelKey ?? own ?? metadata.fields[id]?.i18nKey ?? id
  );
}

/**
 * The pinning a list starts with, from the `pinned` of its column declarations — and what the reset
 * returns to, which is why it is derived rather than written out a second time.
 *
 * The order within an edge follows the declaration, so pinned columns sit left to right as they are
 * declared. An empty edge is left out: TanStack takes `{}` as "nothing pinned", and an empty array
 * would be stored as a change the user never made.
 */
export function defaultPinningOf<Row, M extends EntityMetadata>(
  columns: ColumnDeclaration<Row, M>[]
): { left?: string[]; right?: string[] } {
  const pinning: { left?: string[]; right?: string[] } = {};
  for (const declaration of columns) {
    const edge = declaration.pinned;
    if (!edge) continue;
    (pinning[edge] ??= []).push(columnIdOf(declaration));
  }
  return pinning;
}

/**
 * The columns the page has for this user, i.e. those whose `visible` predicate holds — see
 * `ColumnBase.visible` for why the answer is the backend's and comes as `variables`.
 *
 * Applied before the audit columns are appended and before [defaultPinningOf] runs, so a dropped
 * column is gone from every derivation alike. A declaration without the predicate is kept, which is
 * every column of every page but the task list's three conditional ones.
 */
export function visibleColumnsOf<Row, M extends EntityMetadata>(
  columns: ColumnDeclaration<Row, M>[],
  variables: Record<string, unknown> | undefined
): ColumnDeclaration<Row, M>[] {
  const kept = columns.filter(
    (declaration) =>
      !("visible" in declaration) ||
      (declaration.visible?.({ variables }) ?? true)
  );
  // The same array where nothing was dropped: the columns feed a memo whose identity decides whether
  // TanStack rebuilds every column instance (see useDeclaredColumns).
  return kept.length === columns.length ? columns : kept;
}

/**
 * Which filter the column header offers, derived from the field's data type.
 *
 * `null` means none: a boolean is already a two-value column that the pill filters cover, and a
 * picture has no value to compare. Everything a user reads as a word — including an enum, whose
 * data type is STRING — gets the text filter, which also offers the value list.
 */
export function filterKindFor(dataType: UIDataTypeName): FilterKind | null {
  switch (dataType) {
    case "INT":
    case "LONG":
    case "DECIMAL":
    case "AMOUNT":
      return "number";
    case "DATE":
    case "TIMESTAMP":
    case "TIME":
      return "date";
    case "BOOLEAN":
    case "PICTURE":
      return null;
    default:
      return "text";
  }
}

/**
 * Numbers, counts and amounts are read right-aligned — their digits line up, so a column of sums can
 * be scanned by magnitude. A date or a text is no quantity and stays left.
 */
export function alignFor(dataType: UIDataTypeName): "left" | "right" {
  switch (dataType) {
    case "DECIMAL":
    case "AMOUNT":
    case "INT":
    case "LONG":
      return "right";
    default:
      return "left";
  }
}

/**
 * The message key of a field's label.
 *
 * `i18nKey` is what the entity declares in its `@PropertyInfo`, so the wording matches the rest of
 * ProjectForge and no text is invented in the frontend. A key that is both a leaf and an object in
 * the bundle — `fibu.kost1` is a text of its own *and* the parent of `fibu.kost1.title` — resolves
 * to its exported leaf (see [leafKeyOf]).
 *
 * A field the metadata knows no key for falls back to its name, which is visible enough to be fixed
 * in the entity rather than papered over here.
 *
 * @param hasMessage `t.has` of the translator, i.e. whether the catalogue holds that key.
 * @param override The declaration's own `labelKey`, which wins — for the cases where the entity's
 *   wording doesn't fit the place (a column header that has to be short).
 */
export function labelKeyFor(
  metadata: EntityMetadata,
  name: string,
  hasMessage: (key: string) => boolean,
  override?: string
): string {
  return leafKeyOf(
    override ?? metadata.fields[name]?.i18nKey ?? name,
    hasMessage
  );
}
