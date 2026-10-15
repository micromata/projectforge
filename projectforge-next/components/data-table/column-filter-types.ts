// Column filter values are plain, JSON-serialisable objects so the whole filter
// state can be persisted server-side. A single filterFn routes on the value
// shape: an array means "selection", otherwise the `type` discriminator decides.
//
// Types live apart from the components so filter-fns.ts and the individual filter
// components can share them without importing each other.

/** Filter kind, derived from the column meta the backend sends. */
export type FilterKind = "text" | "number" | "date";

/** What the popover currently shows: the value list, or a comparison for its kind. */
export type FilterMode = "selection" | FilterKind;

export type TextOperator =
  | "contains"
  | "notContains"
  | "equals"
  | "notEqual"
  | "startsWith"
  | "endsWith"
  | "blank"
  | "notBlank";

export type NumberOperator =
  | "equals"
  | "notEqual"
  | "greaterThan"
  | "lessThan"
  | "between"
  | "blank"
  | "notBlank";

export type DateOperator =
  | "equals"
  | "notEqual"
  | "before"
  | "after"
  | "between"
  | "blank"
  | "notBlank";

export interface TextFilterValue {
  type: "text";
  operator: TextOperator;
  value?: string;
}

export interface NumberFilterValue {
  type: "number";
  operator: NumberOperator;
  value?: number;
  valueTo?: number;
}

export interface DateFilterValue {
  type: "date";
  operator: DateOperator;
  /** ISO date (YYYY-MM-DD); compared as a string. */
  value?: string;
  valueTo?: string;
}

/** A selection filter is the list of accepted display values. */
export type SelectionFilterValue = string[];

export type ColumnFilterValue =
  | TextFilterValue
  | NumberFilterValue
  | DateFilterValue
  | SelectionFilterValue;
