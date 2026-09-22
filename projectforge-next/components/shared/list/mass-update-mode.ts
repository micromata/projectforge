import type {
  MassUpdateFieldMeta,
  MassUpdateParameter,
} from "@/lib/rs/multi-select";

/**
 * The single action a mass-update field performs. The backend expresses these as mutually exclusive
 * combinations of flags on [MassUpdateParameter] (see `TextFieldModification.getNewTextValue`); the
 * next frontend picks one from a dropdown so an illegal mix can't be built in the first place.
 *
 * - `set` — overwrite the field with the value.
 * - `append` — add the value to the existing text (only offered when the field allows it).
 * - `replace` — search the field for a text and replace every occurrence with another.
 * - `delete` — clear the field; or, given a search text, delete only its occurrences.
 */
export type MassUpdateMode = "set" | "append" | "replace" | "delete";

/** The modes a field offers, derived from its backend option flags. `set` is always possible. */
export function availableModes(meta: MassUpdateFieldMeta): MassUpdateMode[] {
  const modes: MassUpdateMode[] = ["set"];
  if (meta.appendOption) modes.push("append");
  if (meta.replaceOption) modes.push("replace");
  if (meta.deleteOption) modes.push("delete");
  return modes;
}

/** Which mode a param already represents — used to seed the dropdown from the page's state. */
export function inferMode(param: MassUpdateParameter): MassUpdateMode {
  if (param.delete) return "delete";
  if (param.replaceText != null) return "replace";
  if (param.append) return "append";
  return "set";
}

/**
 * Rebuild a clean param for the chosen mode. The typed value carries across where it still applies,
 * so switching mode keeps what the user typed but never leaves a stale flag or `replaceText` behind.
 */
export function paramForMode(
  mode: MassUpdateMode,
  param: MassUpdateParameter,
  meta: MassUpdateFieldMeta
): MassUpdateParameter {
  const key = meta.valueProperty as keyof MassUpdateParameter;
  const value = param[key];
  const carried = value == null || value === "" ? {} : { [key]: value };
  switch (mode) {
    case "append":
      return { append: true, ...carried };
    case "replace":
      return { ...carried, replaceText: param.replaceText };
    case "delete":
      return { delete: true, ...carried };
    case "set":
    default:
      return { ...carried };
  }
}
