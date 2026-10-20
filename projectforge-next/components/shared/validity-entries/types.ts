import type { ValidationError } from "@/lib/rs/types";

/**
 * One entry with a validity period (see validity-entries-model): a start date, one or more values and a
 * comment. The date and value properties are named by the caller ([ValidityEntriesSectionProps.dateField],
 * [ValidityValueField.name]); `id` is null for an entry not stored yet.
 */
export interface ValidityEntry {
  id?: number | null;
  comment?: string | null;
}

/** How a value is shown and typed. `int` and `decimal` are held as numbers, `select` and `text` as strings. */
export type ValidityValueKind = "int" | "decimal" | "text" | "select";

export interface ValidityValueOption {
  value: string;
  labelKey: string;
}

/** One value column of the table and one input of the dialog. */
export interface ValidityValueField {
  /** Property of the entry, and the `fieldId` the backend names in its validation errors. */
  name: string;
  labelKey: string;
  /** A label built by the caller (`1 EUR =`), shown instead of [labelKey] in the table and the dialog. */
  label?: string;
  /** Shown inside the input of an `int` or `decimal`, after the number (a unit, `USD`). */
  suffix?: string;
  kind: ValidityValueKind;
  /** The choices of a `select`. */
  options?: readonly ValidityValueOption[];
  /** Fixed fraction digits of a `decimal` in the table; the user's typed digits are kept in the dialog. */
  fractionDigits?: number;
  /** Instead of [fractionDigits]: a `decimal` shows its own digits in the table, up to this many. */
  maxFractionDigits?: number;
  /** Marks the field as mandatory; the check itself is the backend's. */
  required?: boolean;
}

/**
 * The answer of a write: the fresh entries (all of them, as the backend now has them), the backend's
 * validation errors (HTTP 406), or a refusal already translated by the backend.
 */
export type ValidityWriteResult<T> =
  | { kind: "ok"; entries: T[] }
  | { kind: "validationErrors"; validationErrors: ValidationError[] }
  | { kind: "rejected"; message: string };

/**
 * Values proposed for a new entry once its date is known (the rates of a currency pair, fetched online), and a
 * note on where they came from. `null` if there is nothing to propose.
 */
export interface ValidityPrefill<T extends ValidityEntry> {
  values?: Partial<T>;
  /** An authored text of the backend (i18n bundle), rendered as markdown. */
  info?: string | null;
}

/** The endpoints the entries are written through; each answers with all entries of the list. */
export interface ValidityEntriesAdapter<T extends ValidityEntry> {
  insert: (entry: T) => Promise<ValidityWriteResult<T>>;
  update: (entry: T) => Promise<ValidityWriteResult<T>>;
  remove: (entry: T) => Promise<ValidityWriteResult<T>>;
}
