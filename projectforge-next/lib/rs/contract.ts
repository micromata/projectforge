/**
 * The calls of the contract page besides the standard REST ones (`ContractEntityRest`): the contract types to
 * choose from and the suggestions of its text fields.
 */

import { request } from "./client";
import { fetchAutoCompletion } from "./dynamic";

/** `ContractEntityRest.ContractTypeOption`: a type of the configuration parameter `orga.contractTypes`. */
export interface ContractTypeOption {
  value: string;
  label: string;
}

/** React Query key of the contract types; they change with the configuration only. */
export const CONTRACT_TYPES_QUERY_KEY = ["contract", "types"] as const;

/** The configured contract types, empty if none are configured. */
export function fetchContractTypes(
  signal?: AbortSignal
): Promise<ContractTypeOption[]> {
  return request<ContractTypeOption[]>(
    "/rs/contract/types",
    { method: "GET" },
    signal
  );
}

/** The text fields of a contract that complete from the values of other contracts (`ContractDao` opts them in). */
export type ContractSuggestedProperty =
  | "title"
  | "coContractorA"
  | "contractPersonA"
  | "signerA"
  | "coContractorB"
  | "contractPersonB"
  | "signerB";

/** The values the other contracts already have for a text field. */
export function fetchContractSuggestions(
  property: ContractSuggestedProperty,
  search: string,
  signal?: AbortSignal
): Promise<string[]> {
  return fetchAutoCompletion<string>(
    `contract/autocomplete?property=${property}&search=:search`,
    search,
    undefined,
    signal
  );
}
