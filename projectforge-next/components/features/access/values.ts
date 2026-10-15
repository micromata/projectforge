import type { AccessValues } from "./schema";
import { ACCESS_TYPES, type AccessEntryDto, type AccessDetail } from "./types";

/** The four matrix rows in [ACCESS_TYPES] order, all flags off — the blank permission matrix. */
function emptyEntries(): AccessEntryDto[] {
  return ACCESS_TYPES.map((accessType) => ({
    accessType,
    accessSelect: false,
    accessInsert: false,
    accessUpdate: false,
    accessDelete: false,
  }));
}

/**
 * Normalises the loaded entity's access entries to the fixed four rows in [ACCESS_TYPES] order: a type
 * the backend left out (a partially filled entry, or a fresh preset) becomes an all-false row, so the
 * matrix always renders its full four lines and the form values line up with `accessEntries[i]`.
 */
export function normalizeEntries(
  entries: AccessEntryDto[] | null | undefined
): AccessEntryDto[] {
  const byType = new Map((entries ?? []).map((e) => [e.accessType, e]));
  return ACCESS_TYPES.map((accessType) => {
    const found = byType.get(accessType);
    return {
      accessType,
      accessSelect: found?.accessSelect ?? false,
      accessInsert: found?.accessInsert ?? false,
      accessUpdate: found?.accessUpdate ?? false,
      accessDelete: found?.accessDelete ?? false,
    };
  });
}

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined`.
 */
export function toFormValues(detail: AccessDetail): AccessValues {
  return {
    id: detail.id ?? null,
    group: detail.group ?? null,
    task: detail.task ?? null,
    recursive: detail.recursive ?? true,
    description: detail.description ?? null,
    accessEntries: normalizeEntries(detail.accessEntries),
  };
}

/** Blank form for an access entry that doesn't exist yet — recursive on, as GroupTaskAccessDO defaults it. */
export function emptyAccessValues(): AccessValues {
  return {
    id: null,
    group: null,
    task: null,
    recursive: true,
    description: null,
    accessEntries: emptyEntries(),
  };
}
