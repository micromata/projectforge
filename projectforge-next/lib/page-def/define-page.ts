/**
 * The two rules that turn a field of the generated metadata into a column or a form field, plus the
 * identity function that binds a page declaration to its entity's field names.
 *
 * They are kept apart from the renderers so they can be tested without a DOM (see vitest.config.mts):
 * what is worth asserting is the derivation, not the JSX around it.
 */

import type { FilterKind } from "@/components/data-table";
import type { EntityMetadata, UIDataTypeName } from "@/lib/metadata/types";
import type { EntityWithId } from "@/hooks/use-entity-detail";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { PageDef } from "./types";

/**
 * Binds a declaration to one entity: every `name` is checked against `keyof metadata.fields`, so a
 * field renamed in the entity fails the typecheck instead of silently rendering an empty column.
 *
 * Nothing else happens here — the declaration is the value, and both renderers read it as it is.
 */
export function definePage<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>(def: PageDef<Row, Values, Data, M>): PageDef<Row, Values, Data, M> {
  return def;
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
 * the bundle — `fibu.kost1` is a text of its own *and* the parent of `fibu.kost1.title` — is
 * exported by the generator as `<key>._`, because a JSON object cannot also be a string. Hence the
 * fallback: where `<key>._` exists, that is the leaf.
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
  const base = override ?? metadata.fields[name]?.i18nKey ?? name;
  return hasMessage(`${base}._`) ? `${base}._` : base;
}
