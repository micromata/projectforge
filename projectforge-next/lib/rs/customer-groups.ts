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

/** Mirror of `CustomerGroupService.UnassignedEntry`. */
export interface UnassignedEntry {
  /** The group's name, the customer as "number name", or the free text. */
  name: string;
  kind: "GROUP" | "CUSTOMER" | "FREE_TEXT";
  /** The year of the latest order or invoice (a group: of any of its members). */
  year: number;
}

/**
 * Mirror of `CustomerGroupService.Unassigned`: sorted by year (most recent first), then by name. Spring omits
 * an empty list (`NON_NULL`).
 */
export interface UnassignedCustomers {
  entries?: UnassignedEntry[];
}

/**
 * What the unsaved configuration leaves without a business unit: only customers and free texts of the
 * last five years' orders and invoices count. Nothing is stored.
 */
export function fetchUnassignedCustomers(
  values: CustomerGroupsValues,
  signal?: AbortSignal
): Promise<UnassignedCustomers> {
  return request<UnassignedCustomers>(
    "/rs/customerGroups/unassigned",
    { method: "POST", body: JSON.stringify(values) },
    signal
  );
}

/** Mirror of `CustomerGroupService.BusinessUnitMember`. */
export interface BusinessUnitMember {
  /** The group's name, the customer as "number name", or the free text. */
  name: string;
  kind: UnassignedEntry["kind"];
  /** Only through the tasks of its projects: the customer itself belongs to no business unit. */
  viaTask: boolean;
}

/**
 * What each business unit of the unsaved configuration stands for in the last five years' orders and
 * invoices, by business-unit key; a business unit nothing counts to is missing. Nothing is stored.
 */
export function fetchBusinessUnitMembers(
  values: CustomerGroupsValues,
  signal?: AbortSignal
): Promise<Record<string, BusinessUnitMember[]>> {
  return request<Record<string, BusinessUnitMember[]>>(
    "/rs/customerGroups/businessUnitMembers",
    { method: "POST", body: JSON.stringify(values) },
    signal
  );
}
