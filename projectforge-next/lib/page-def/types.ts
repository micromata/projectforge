/**
 * One entry in the colour legend below a list table.
 *
 * The `className` is one of the `row-*` tokens defined in `globals.css`; `labelKey` is a full
 * i18n key. Set `strikethrough` for entries whose rows are also struck through (row-deleted).
 */
export interface LegendEntry {
  className: string;
  labelKey: string;
  strikethrough?: boolean;
}

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
import type { EntityMetadata, UIDataTypeName } from "@/lib/metadata/types";
import type { EntityWithId } from "@/hooks/use-entity-detail";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { MagicFilter } from "@/lib/rs/types";

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
  /**
   * Frozen to that edge of the table until the user unpins it — for the columns that identify the row
   * and have to stay readable while the rest is scrolled sideways (an order's number, customer,
   * project and title).
   *
   * The starting point only: pinning is state the user owns, stored per user and entity, and this is
   * what a reset returns to (see `defaultPinningOf`).
   */
  pinned?: "left" | "right";
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
  /**
   * What kind of value it is, where that is not plain text: an order's net sum is money and reads as
   * money (right-aligned, in the user's currency), although no field of the entity carries it — the
   * sums are computed (`OrderInfo`) and transient.
   *
   * Only for computed columns, and only because there is no metadata to derive it from; a column of a
   * real field must never state it, or the entity and the list could say different things.
   */
  dataType?: UIDataTypeName;
}

/**
 * Two date (or timestamp) fields of the entity shown as the one value they are: a period under a
 * single label, both ends optional — the column counterpart of [DatePeriodDeclaration].
 *
 * A period is not a data type of its own and must not become one, for the same reason it isn't one in
 * a form: the entity has two properties, the metadata reports two dates, and the backend sorts by one
 * of them (`begin`, which is therefore the column's id). What is shared is only how it is *shown*.
 *
 * No column filter: the two ends are one question and the backend answers it as one — an order's
 * period of performance is a single window matched with overlap semantics
 * (`OrderEntityRest.addMagicFilterElements`), and a client-side filter over the rendered text would
 * only be a second, weaker one.
 */
export interface PeriodColumn<M extends EntityMetadata> extends Omit<
  ColumnBase<never>,
  "cell" | "filterKind" | "align" | "labelKey"
> {
  /** Label of the period as a whole, e.g. `fibu.periodOfPerformance`. */
  periodLabelKey: string;
  begin: FieldNameOf<M>;
  end: FieldNameOf<M>;
}

export type ColumnDeclaration<Row, M extends EntityMetadata> =
  | FieldColumn<Row, M>
  | ComputedColumn<Row>
  | PeriodColumn<M>;

interface FieldBase {
  /** Columns of the section's grid this field spans. */
  span?: 1 | 2 | 3;
  /**
   * Starts a new row of the section's grid, leaving the rest of the current one empty — for a field that
   * begins a group a reader is meant to see as one line (an order's three dates around the assignment,
   * then its period, deadline and probability).
   *
   * Needed because the grid fills row by row: the field before may end anywhere, and a row of dates that
   * begins in the last column and continues on the next line is not the line it was declared as. Only on
   * the three-column layout — stacked on a phone every field is its own row anyway.
   */
  startsRow?: boolean;
}

/** A form field of the entity: which one, where, and how it is labelled. */
export interface DeclaredField<M extends EntityMetadata> extends FieldBase {
  name: FieldNameOf<M>;
  labelKey?: string;
  hintKey?: string;
  /** Renders a textarea of this many rows instead of a single-line input. */
  rows?: number;
  /**
   * Digits of the widest value a numeric field can hold, so its box is that wide and no wider — six
   * for an order's number, three for a percentage. See NumberField's `maxDigits` for why this is
   * declared rather than derived.
   */
  maxDigits?: number;
  /**
   * Right-aligns a numeric field's digits, where a form does line up numbers to be compared. The
   * default is left, like every other field of the form — unlike a *column*, which aligns on the right
   * (see `align` above and NumberField's own).
   */
  alignNumber?: "left" | "right";
  /** Larger and in the accent colour — for the one value a reader looks for first. */
  emphasized?: boolean;
  /**
   * Shown but not editable — a value the backend assigns and the user only reads, like an order's
   * number (`AuftragDao.getNextNumber` on the first save). Hiding it would be worse: it is how an
   * order is referred to in every conversation.
   */
  readOnly?: boolean;
}

/**
 * Two date fields of the entity shown as the one value they are: a period, under a single label, both
 * ends optional (see DatePeriodField).
 *
 * A period is not a data type of its own and must not become one: the entity has two `LocalDate`
 * properties, the metadata reports two dates, and the schema validates two dates. What is shared is
 * only how they are *shown* — which is exactly what a declaration decides.
 */
export interface DatePeriodDeclaration<
  M extends EntityMetadata,
> extends FieldBase {
  /** Label of the period as a whole, e.g. `fibu.periodOfPerformance._`. */
  periodLabelKey: string;
  begin: FieldNameOf<M>;
  end: FieldNameOf<M>;
  hintKey?: string;
}

/** A field the declaration cannot describe: a cost number, a keyword picker. */
export interface CustomField extends FieldBase {
  custom: ComponentType<{ className?: string }>;
}

/**
 * Two or three narrow fields sharing **one cell** of the section's grid, side by side — an order's
 * number and the date of the offer it came from.
 *
 * Not a period ([DatePeriodDeclaration]) and not one value: each member keeps its own label, its own
 * error line and its own metadata. What the grouping says is only that neither of them needs a third
 * of the page — a number bounded to six digits leaves room for a date beside it — and that putting
 * them in cells of their own would push the field after them into the next row, breaking the three
 * columns the rest of the section reads in.
 *
 * A member declaring `maxDigits` stays as wide as its box (that is what the digit count is for); the
 * others share what is left. Below the width of one grid column the members stack, for the same reason
 * the two ends of a period do: squeezed side by side they would truncate the values they show.
 */
export interface FieldGroupDeclaration<
  M extends EntityMetadata,
> extends FieldBase {
  group: DeclaredField<M>[];
}

export type FieldDeclaration<M extends EntityMetadata> =
  | DeclaredField<M>
  | DatePeriodDeclaration<M>
  | FieldGroupDeclaration<M>
  | CustomField;

/**
 * One card of the edit page, and one anchor tab above it (EditPageShell couples them positionally).
 */
export interface SectionDef<M extends EntityMetadata> {
  id: string;
  titleKey: string;
  /**
   * Shorter label for the tab above, where the card's heading would not fit ("Allgemein" vs
   * "Allgemeine Informationen"). Defaults to `titleKey`.
   */
  tabTitleKey?: string;
  fields?: FieldDeclaration<M>[];
  /** Renders the whole body itself — a book's loan block, its attachments. */
  render?: (ctx: { id: number | null }) => ReactNode;
}

/**
 * A page of the entity beside the form and the history — an order's forecast analysis.
 *
 * Declared rather than built: its route follows the same convention the history's does
 * (`${route}/${id}/${id of the tab}`), and its label is a message key like every other label here, so a
 * declaration stays a value and needs no translator (see entityTabs).
 */
export interface ExtraTabDef {
  id: string;
  labelKey: string;
}

export interface EditDef<Values, Data, M extends EntityMetadata> {
  /** Zod schema of the form, built from the metadata (lib/validation/from-metadata.ts). */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  schema: ZodType<any, any>;
  /** Names of the fields the form holds, so a server error naming another one becomes a toast. */
  fieldNames: readonly string[];
  /**
   * Names of the array (collection) fields. A bare server error on one of these (e.g. "order has no
   * positions") has no mounted `<form.Field>` to display it, so it surfaces as a toast instead.
   * Indexed paths (`positionen[0].titel`) still land on the row's field as normal.
   */
  arrayFieldNames?: readonly string[];
  /** Values of a new, empty entry. */
  defaultValues: () => Values;
  toFormValues: (data: Data) => Values;
  /** Heading of an existing entry, e.g. the book's title. */
  title: (data: Data) => string;
  /** Heading while adding one, e.g. `books.edit.newTitle`. */
  newTitleKey: string;
  savedMessageKey: string;
  sections: SectionDef<M>[];
  /**
   * Names of the writes the entity offers besides save — `["lendOut", "returnBook"]` for a book.
   *
   * They run through the form's own submit, i.e. the same Zod validation, values and 406 handling
   * (see lib/rs/submit-meta.ts); declaring the names is all the renderer needs to route
   * `meta.action` to `/rs/{entity}/{action}` instead of `saveorupdate`. What *triggers* them stays
   * the entity's own business: a section's `render` puts the buttons where they belong.
   */
  actions?: readonly string[];
  /**
   * Beside the heading of the edit page — a badge saying whether the book is lent out. Rendered on
   * the form and on the pages of its own alike, hence the entity rather than the form values.
   */
  headerTrailing?: (data: Data | undefined) => ReactNode;
  /**
   * Right of the save button — an order's "send an e-mail notification?" checkbox.
   *
   * A component rather than a field declaration, because what belongs here is not a property of the
   * entity but a choice about the save itself, and it is read where the save is triggered. Rendered
   * inside the form, so it may bind to a form value like any field ([CheckboxField]).
   */
  saveOption?: ComponentType;
  /**
   * Sticky banner between the tab strip and the scrollable sections — always visible while the user
   * scrolls (e.g. an order's number, status, forecast type and running sums).
   *
   * Rendered inside the form so it can subscribe to live form values via `useEntityEditForm`.
   */
  editBanner?: ComponentType;
  /** Further tabs leading to a page of their own. Appended after the history. */
  extraTabs?: ExtraTabDef[];
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
  /**
   * Colour legend shown below the table. `row-deleted` (struck-through, deleted entries) is
   * always added as the first entry; set `legend` only for additional, entity-specific colours.
   */
  legend?: LegendEntry[];

  /**
   * Overrides the label of the always-present `row-deleted` legend entry.
   *
   * Defaults to `table.legend.deleted` ("Deleted"). Use when the page applies `row-deleted` to
   * more states than just hard-deleted rows — e.g. the order book colours ABGELEHNT and ERSETZT
   * the same way and needs "Deleted / rejected / replaced".
   */
  deletedLabelKey?: string;

  /**
   * Row highlight class, e.g. `"row-red"` — the semantic classes in `globals.css`.
   *
   * Called once per row; return `undefined` for no highlight. Mirrors the `rowClassName` prop of
   * `DataTable` and uses the same CSS tokens (`row-deleted`, `row-red`, `row-green`, `row-blue`).
   */
  rowClassName?: (row: Row) => string | undefined;
  /**
   * Renders what the backend aggregated over the whole result set, between the toolbar and the table —
   * the sums of the order book (`ResultSet.statistics`, see OrderStatisticsLine).
   *
   * The value is `unknown` because its shape belongs to the entity's rest class, and this is the one
   * place that knows which: the declaration names the component, so it narrows there and nothing
   * generic has to carry a type it cannot check.
   */
  statistics?: (ctx: { statistics: unknown; isFetching: boolean }) => ReactNode;
  /**
   * Actions on the whole list, rendered in the toolbar left of the gear menu — the Excel and forecast
   * exports of the order book (see OrderListActions).
   *
   * It is handed the filter the list call sends, so an export acts on exactly the rows the table shows.
   * A component rather than a list of declarations: an export may need a dialog of its own, and what it
   * asks for there belongs to the entity, not to this shell.
   */
  listActions?: ComponentType<{ filter: MagicFilter }>;
  edit: EditDef<Values, Data, M>;
}
