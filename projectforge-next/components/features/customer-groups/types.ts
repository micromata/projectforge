import type { EntityRef } from "@/components/shared/entity-autocomplete";
import type { ArrayRow } from "@/lib/field-array";

/**
 * Mirror of `CustomerGroupPageRest.CustomerSetData`: the members a group and a business unit share.
 * Spring omits empty fields (`NON_NULL`), hence the optional ones; [toFormValues] fills them in.
 */
export interface CustomerSetData {
  /** Stable key the filters refer to (`g:<key>`, `b:<key>`); assigned here for a new row. */
  key?: string | null;
  name?: string | null;
  /** Customer entities, as the multi-autocomplete holds them. */
  customers?: EntityRef[];
  /** Free-text customers: exact names or patterns (`ACME*`, `*Logistics`, `*ACME*`). */
  texts?: string[];
}

export type CustomerGroupData = CustomerSetData;

export interface BusinessUnitData extends CustomerSetData {
  /** Keys of the customer groups belonging to the business unit. */
  groups?: string[];
  /** Tasks whose projects count to the business unit if their customer leads to none. */
  tasks?: EntityRef[];
}

/** Mirror of `CustomerGroupPageRest.CustomerGroupsData` (GET `/rs/customerGroups`). */
export interface CustomerGroupsData {
  groups?: CustomerGroupData[];
  businessUnits?: BusinessUnitData[];
  /** Epoch millis of the stored value, sent back on save for the optimistic lock. */
  lastUpdate?: number | null;
}

/**
 * A row of the form. An [ArrayRow] without an id ever set: the sets are no entities, so removing one
 * drops it from the list instead of soft-deleting it (see `removeRow`).
 */
export interface CustomerSetValues extends ArrayRow {
  key: string;
  name: string;
  customers: EntityRef[];
  texts: string[];
}

export interface BusinessUnitValues extends CustomerSetValues {
  groups: string[];
  tasks: EntityRef[];
}

export interface CustomerGroupsValues {
  groups: CustomerSetValues[];
  businessUnits: BusinessUnitValues[];
  lastUpdate: number | null;
}
