/**
 * What a list and an edit page of one entity are made of, as data.
 *
 * A declaration says **what is shown, in which order, how wide and under which label**. It never
 * says whether a field is mandatory, how long its value may be or which values an enum has — those
 * are the backend's rules and come from the generated metadata (`lib/metadata/*.generated.ts`), the
 * same source the Zod schema reads. That is why there is no `required` here, and there must not be:
 * a second place to declare it is how the form and the entity drifted apart before.
 *
 * Everything genuinely specific keeps full JSX freedom: a column may bring its own `cell`, a field
 * its own component, a section its own body. The escape hatch is first class, not a last resort.
 */

import type { ComponentType, ReactNode } from "react";
import type { CellContext } from "@tanstack/react-table";
import type { ZodType } from "zod";
import type { FilterKind } from "@/components/data-table";
import type { EditPageTab } from "@/components/shared/edit-page-tabs";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { EntityWithId } from "@/hooks/use-entity-detail";
import type { ListRow } from "@/hooks/use-entity-list-page";

/** The field names of an entity, so a typo in a declaration fails the typecheck. */
export type FieldNameOf<M extends EntityMetadata> = keyof M["fields"] & string;

interface ColumnBase<Row> {
  /** Width in pixels. The fixed table layout ignores `minSize`, so this is what counts. */
  size?: number;
  minSize?: number;
  /** Extra classes of the default cell; ignored when `cell` is given. */
  className?: string;
  /** Overrides the alignment derived from the data type (numbers right, everything else left). */
  align?: "left" | "right";
  /** Overrides the filter kind derived from the data type; `null` offers no filter at all. */
  filterKind?: FilterKind | null;
  /** Renders the cell itself, instead of the default for the field's data type. */
  cell?: (ctx: CellContext<Row, unknown>) => ReactNode;
  /** Overrides the label derived from the field's `i18nKey`. */
  labelKey?: string;
  /** Shorter label for the header, where the full one would not fit ("Anh." vs "Anhänge"). */
  headerLabelKey?: string;
}

/** A column showing one property of the entity, labelled from its metadata. */
export interface FieldColumn<
  Row,
  M extends EntityMetadata,
> extends ColumnBase<Row> {
  name: FieldNameOf<M>;
}

/** A column the entity has no field for — a value composed from the row, or a nested property. */
export interface ComputedColumn<Row> extends ColumnBase<Row> {
  /** Must be the property the backend sorts by; there is no field to take it from. */
  id: string;
  labelKey: string;
  accessor: (row: Row) => unknown;
}

export type ColumnDeclaration<Row, M extends EntityMetadata> =
  | FieldColumn<Row, M>
  | ComputedColumn<Row>;

interface FieldBase {
  /** Columns of the section's grid this field spans. */
  span?: 1 | 2 | 3;
}

/** A form field of the entity: which one, where, and how it is labelled. */
export interface DeclaredField<M extends EntityMetadata> extends FieldBase {
  name: FieldNameOf<M>;
  labelKey?: string;
  hintKey?: string;
  /** Renders a textarea of this many rows instead of a single-line input. */
  rows?: number;
  /** Larger and in the accent colour — for the one value a reader looks for first. */
  emphasized?: boolean;
}

/** A field the declaration cannot describe: a cost number, a keyword picker. */
export interface CustomField extends FieldBase {
  custom: ComponentType<{ className?: string }>;
}

export type FieldDeclaration<M extends EntityMetadata> =
  | DeclaredField<M>
  | CustomField;

/**
 * One card of the edit page, and one anchor tab above it (EditPageShell couples them positionally).
 */
export interface SectionDef<M extends EntityMetadata> {
  id: string;
  titleKey: string;
  fields?: FieldDeclaration<M>[];
  /** Renders the whole body itself — a book's loan block, its attachments. */
  render?: (ctx: { id: number | null }) => ReactNode;
}

export interface EditDef<Values, Data, M extends EntityMetadata> {
  /** Zod schema of the form, built from the metadata (lib/validation/from-metadata.ts). */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  schema: ZodType<any, any>;
  /** Names of the fields the form holds, so a server error naming another one becomes a toast. */
  fieldNames: readonly string[];
  /** Values of a new, empty entry. */
  defaultValues: () => Values;
  toFormValues: (data: Data) => Values;
  /** Heading of an existing entry, e.g. the book's title. */
  title: (data: Data) => string;
  /** Heading while adding one, e.g. `books.edit.newTitle`. */
  newTitleKey: string;
  savedMessageKey: string;
  sections: SectionDef<M>[];
  /** Tabs leading to a page of their own, e.g. the history. Appended after the anchor tabs. */
  extraTabs?: (id: number) => EditPageTab[];
}

export interface PageDef<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
> {
  /** REST category, e.g. `cost1` — what every call and the stored column state are keyed by. */
  entity: string;
  metadata: M;
  /** Route of the list, e.g. `/cost1`; the edit page is `${route}/${id}`. */
  route: string;
  /** React Query key of the list, e.g. `["cost1"]`. */
  queryKey: readonly unknown[];
  /** The menu parent above the title, e.g. `menu.fibu`. */
  categoryKey: string;
  titleKey: string;
  addTitleKey: string;
  searchPlaceholderKey: string;
  columns: ColumnDeclaration<Row, M>[];
  edit: EditDef<Values, Data, M>;
}
