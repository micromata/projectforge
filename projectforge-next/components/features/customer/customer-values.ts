import type { CustomerValues } from "./customer-schema";
import type { CustomerDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` — which a
 * controlled input would read as "uncontrolled" and the schema as a missing value. The mandatory
 * `name` becomes "" rather than null (an emptied required input keeps "").
 */
export function toFormValues(customer: CustomerDetail): CustomerValues {
  return {
    id: customer.id ?? null,
    nummer: customer.nummer ?? null,
    name: customer.name ?? "",
    identifier: customer.identifier ?? null,
    division: customer.division ?? null,
    status: customer.status ?? null,
    description: customer.description ?? null,
    konto: customer.konto ?? null,
    kost: customer.kost ?? null,
    created: customer.created ?? null,
  };
}

/**
 * Blank form for a customer that doesn't exist yet.
 *
 * `nummer` starts empty: the number is the user's to assign, and a proposed 0 would be a valid but
 * unintended one. `status` starts unset like the entity's (`KundeDO.status` is nullable).
 */
export function emptyCustomerValues(): CustomerValues {
  return {
    id: null,
    nummer: null,
    name: "",
    identifier: null,
    division: null,
    status: null,
    description: null,
    konto: null,
    kost: null,
    created: null,
  };
}
