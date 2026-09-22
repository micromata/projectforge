import type { FilterFn, Row } from "@tanstack/react-table";
import type {
  ColumnFilterValue,
  DateFilterValue,
  NumberFilterValue,
  TextFilterValue,
} from "./column-filter-types";

/** Renders a cell value the way the selection filter lists it. */
export function toFilterText(value: unknown): string {
  if (value == null) return "";
  if (Array.isArray(value)) return value.map(toFilterText).join(", ");
  if (typeof value === "object") {
    const displayName = (value as { displayName?: unknown }).displayName;
    return displayName == null ? "" : String(displayName);
  }
  return String(value);
}

/** Sort order of the selection list: blanks last, the rest by the user's locale. */
export function compareFilterText(a: string, b: string): number {
  if (a === "") return 1;
  if (b === "") return -1;
  return a.localeCompare(b);
}

/** Normalises epoch millis or an ISO timestamp to YYYY-MM-DD. */
export function toDateString(value: unknown): string | null {
  if (value == null || value === "") return null;
  if (typeof value === "number") {
    return new Date(value).toISOString().slice(0, 10);
  }
  const text = String(value);
  // Already a plain or leading ISO date.
  if (/^\d{4}-\d{2}-\d{2}/.test(text)) return text.slice(0, 10);
  const parsed = new Date(text);
  return Number.isNaN(parsed.getTime())
    ? null
    : parsed.toISOString().slice(0, 10);
}

function isBlank(value: unknown): boolean {
  return value == null || toFilterText(value) === "";
}

function matchesSelection(value: unknown, accepted: string[]): boolean {
  if (Array.isArray(value)) {
    if (value.length === 0) return accepted.includes("");
    return value.some((entry) => accepted.includes(toFilterText(entry)));
  }
  return accepted.includes(toFilterText(value));
}

function matchesText(value: unknown, filter: TextFilterValue): boolean {
  if (filter.operator === "blank") return isBlank(value);
  if (filter.operator === "notBlank") return !isBlank(value);

  const needle = filter.value?.toLowerCase() ?? "";
  if (needle === "") return true;
  const haystack = toFilterText(value).toLowerCase();

  switch (filter.operator) {
    case "contains":
      return haystack.includes(needle);
    case "notContains":
      return !haystack.includes(needle);
    case "equals":
      return haystack === needle;
    case "notEqual":
      return haystack !== needle;
    case "startsWith":
      return haystack.startsWith(needle);
    case "endsWith":
      return haystack.endsWith(needle);
    default:
      return true;
  }
}

function matchesNumber(value: unknown, filter: NumberFilterValue): boolean {
  if (filter.operator === "blank") return isBlank(value);
  if (filter.operator === "notBlank") return !isBlank(value);
  if (filter.value == null) return true;

  const numeric = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(numeric)) return false;

  switch (filter.operator) {
    case "equals":
      return numeric === filter.value;
    case "notEqual":
      return numeric !== filter.value;
    case "greaterThan":
      return numeric > filter.value;
    case "lessThan":
      return numeric < filter.value;
    case "between":
      return (
        numeric >= filter.value &&
        (filter.valueTo == null || numeric <= filter.valueTo)
      );
    default:
      return true;
  }
}

function matchesDate(value: unknown, filter: DateFilterValue): boolean {
  if (filter.operator === "blank") return isBlank(value);
  if (filter.operator === "notBlank") return !isBlank(value);
  if (!filter.value) return true;

  const date = toDateString(value);
  if (date == null) return false;

  switch (filter.operator) {
    case "equals":
      return date === filter.value;
    case "notEqual":
      return date !== filter.value;
    case "before":
      return date < filter.value;
    case "after":
      return date > filter.value;
    case "between":
      return (
        date >= filter.value && (!filter.valueTo || date <= filter.valueTo)
      );
    default:
      return true;
  }
}

/**
 * Single filterFn for every column; dispatches on the filter value's shape.
 * Generic in TData because it only ever reads the cell value.
 */
export function universalFilterFn<TData>(
  row: Row<TData>,
  columnId: string,
  filterValue: ColumnFilterValue
): boolean {
  if (filterValue == null) return true;
  const value = row.getValue(columnId);

  if (Array.isArray(filterValue)) return matchesSelection(value, filterValue);

  switch (filterValue.type) {
    case "text":
      return matchesText(value, filterValue);
    case "number":
      return matchesNumber(value, filterValue);
    case "date":
      return matchesDate(value, filterValue);
    default:
      return true;
  }
}

/** Cast helper for the places TanStack wants a concretely typed FilterFn. */
export function universalFilterFnFor<TData>(): FilterFn<TData> {
  return universalFilterFn as FilterFn<TData>;
}
