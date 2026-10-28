/**
 * The JSON value of the configuration parameter `orga.contractTypes`, mirroring `ContractType`
 * (projectforge-business): the types selectable in the contracts, as array of value and label. The backend
 * validates it on saving.
 */
export const CONTRACT_TYPES_PARAM = "orga.contractTypes";

export interface ContractTypeEntry {
  /** Short form stored in the contract, e.g. `NDA`. */
  value: string;
  /** Shown in the selection, e.g. `Non-disclosure agreement`. */
  label: string;
}

export const EMPTY_CONTRACT_TYPE: ContractTypeEntry = { value: "", label: "" };

/** The entries of the stored value, none for an empty one. `invalid` if the value isn't a JSON array. */
export function parseContractTypesConfig(json: string | null | undefined): {
  entries: ContractTypeEntry[];
  invalid: boolean;
} {
  if (!json?.trim()) {
    return { entries: [], invalid: false };
  }
  try {
    const value: unknown = JSON.parse(json);
    if (!Array.isArray(value)) {
      return { entries: [], invalid: true };
    }
    return {
      entries: value
        .filter(
          (entry): entry is Record<string, unknown> =>
            typeof entry === "object" && entry !== null
        )
        .map((entry) => ({
          value: typeof entry.value === "string" ? entry.value : "",
          label: typeof entry.label === "string" ? entry.label : "",
        })),
      invalid: false,
    };
  } catch {
    return { entries: [], invalid: true };
  }
}

/**
 * The JSON value of the entries (`[{"value":"NDA","label":"…"}]`), empty if there are none. Not trimmed:
 * it's written on every keystroke, the backend trims.
 */
export function serializeContractTypesConfig(entries: ContractTypeEntry[]): string {
  return entries.length === 0
    ? ""
    : JSON.stringify(entries.map((entry) => ({ value: entry.value, label: entry.label })));
}
