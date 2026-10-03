import { request } from "./client";
import { postEntityAction, type EntityWriteResult } from "./entity";
import type { ValidationError } from "./types";
import type {
  CustomerGroupsData,
  CustomerGroupsValues,
} from "@/components/features/customer-groups/types";

/** The customer groups and business units (finance and controlling only, 403 otherwise). */
export function fetchCustomerGroups(
  signal?: AbortSignal
): Promise<CustomerGroupsData> {
  return request<CustomerGroupsData>(
    "/rs/customerGroups",
    { method: "GET" },
    signal
  );
}

/** Saves the whole configuration; HTTP 406 carries the `validationErrors` (see lib/rs/entity.ts). */
export function saveCustomerGroups(
  values: CustomerGroupsValues
): Promise<EntityWriteResult> {
  return postEntityAction("customerGroups", "save", values);
}

/** Mirror of `CustomerGroupService.CustomerMatches`; Spring omits an empty list (`NON_NULL`). */
export interface CustomerMatches {
  /** Customer entities matched, as "number name". */
  customers?: string[];
  /** Free texts of orders and invoices matched. */
  freeTexts?: string[];
}

/** What the given customer names and patterns match, before they are saved. */
export function fetchCustomerMatches(
  texts: string[],
  signal?: AbortSignal
): Promise<CustomerMatches> {
  return request<CustomerMatches>(
    "/rs/customerGroups/matches",
    { method: "POST", body: JSON.stringify({ texts }) },
    signal
  );
}

/**
 * The errors the unsaved configuration would be refused with on saving — already translated, with the
 * field of each set involved in a conflict. Nothing is stored.
 */
export function validateCustomerGroups(
  values: CustomerGroupsValues,
  signal?: AbortSignal
): Promise<ValidationError[]> {
  return request<ValidationError[]>(
    "/rs/customerGroups/validate",
    { method: "POST", body: JSON.stringify(values) },
    signal
  );
}
