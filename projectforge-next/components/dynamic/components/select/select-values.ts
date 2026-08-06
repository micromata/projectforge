import { getByPath, type DataObject } from "@/lib/dynamic/path";
import type { AutoCompletion, DynamicLayoutNode } from "@/lib/rs/types";

/** One option of a SELECT, reduced to the two strings the ui needs. */
export interface SelectOption {
  value: string;
  label: string;
}

/** The parts of a UISelect element the components need. */
export interface SelectSpec {
  id: string;
  multi: boolean;
  /** Property of an option/value object holding the id. UISelect defaults it to "id". */
  valueProperty: string;
  /** Property holding the display text. UISelect defaults it to "displayName". */
  labelProperty: string;
  options: SelectOption[];
  autoCompletion?: AutoCompletion;
  /**
   * True if the field holds whole entities rather than plain ids. The backend marks those by
   * giving the autoCompletion a `type` (USER, GROUP, …) - see DynamicReactSelect of the legacy
   * renderer, which keys its onChange on exactly this.
   */
  storesObjects: boolean;
}

export function selectSpecOf(node: DynamicLayoutNode): SelectSpec {
  const valueProperty = (node.valueProperty as string) ?? "id";
  const labelProperty = (node.labelProperty as string) ?? "displayName";
  const autoCompletion = node.autoCompletion as AutoCompletion | undefined;
  const rawValues = (node.values as DataObject[] | undefined) ?? [];
  return {
    id: node.id as string,
    multi: node.multi === true,
    valueProperty,
    labelProperty,
    autoCompletion,
    storesObjects: autoCompletion?.type != null,
    options: rawValues
      .map((entry) => toOption(entry, valueProperty, labelProperty))
      .filter((option): option is SelectOption => option != null),
  };
}

/**
 * Turns a value of the data or of `values` into an option.
 *
 * The shape varies: `UISelectValue` sends `{ id, displayName }`, an entity reference sends the
 * whole DTO, and an enum field sends a bare string.
 */
export function toOption(
  raw: unknown,
  valueProperty: string,
  labelProperty: string
): SelectOption | null {
  if (raw == null) return null;
  if (typeof raw !== "object") {
    const value = String(raw);
    return { value, label: value };
  }
  const entry = raw as DataObject;
  const value = entry[valueProperty] ?? entry.id ?? entry.value;
  if (value == null) return null;
  const label =
    entry[labelProperty] ?? entry.displayName ?? entry.label ?? String(value);
  return { value: String(value), label: String(label) };
}

/** The currently selected options, read from the data at the element's path. */
export function selectedOptions(
  data: DataObject,
  spec: SelectSpec
): SelectOption[] {
  const raw = getByPath(data, spec.id);
  const entries = spec.multi ? (Array.isArray(raw) ? raw : []) : [raw];
  return entries
    .map((entry) => {
      const option = toOption(entry, spec.valueProperty, spec.labelProperty);
      // A plain id has no label of its own; look it up among the offered values.
      if (option && option.value === option.label) {
        const known = spec.options.find((it) => it.value === option.value);
        if (known) return known;
      }
      return option;
    })
    .filter((option): option is SelectOption => option != null);
}

/**
 * Builds the value to store for the given options: plain ids for an enum-like select, whole
 * objects for an entity select (the backend deserializes those back into references).
 */
export function toDataValue(
  options: SelectOption[],
  spec: SelectSpec
): unknown {
  const encode = (option: SelectOption) =>
    spec.storesObjects
      ? {
          [spec.valueProperty]: coerceId(option.value),
          [spec.labelProperty]: option.label,
        }
      : coerceId(option.value);
  return spec.multi
    ? options.map(encode)
    : options[0]
      ? encode(options[0])
      : null;
}

/** Entity ids are numbers on the wire; enum values and language codes are strings. */
function coerceId(value: string): string | number {
  return /^-?\d+$/.test(value) ? Number(value) : value;
}
