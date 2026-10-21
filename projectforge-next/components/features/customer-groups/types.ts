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

/**
 * Mirror of `CustomerGroupPageRest.OrderAccessData`: read access to all orders of the customers, groups
 * and business units for the users and the members of the user groups (project managers and assistants
 * only).
 */
export interface OrderAccessData {
  key?: string | null;
  name?: string | null;
  remark?: string | null;
  users?: EntityRef[];
  /** Groups of users, not customer groups. */
  userGroups?: EntityRef[];
  customers?: EntityRef[];
  /** Keys of the customer groups. */
  groups?: string[];
  /** Keys of the business units. */
  businessUnits?: string[];
}

/**
 * Mirror of `CustomerGroupPageRest.ProjectGroupData`: projects offered as one entry in the project
 * filters of the lists.
 */
export interface ProjectGroupData {
  /** Stable key the filters refer to (`pg:<key>`). */
  key?: string | null;
  name?: string | null;
  projects?: EntityRef[];
}

/** Mirror of `CustomerGroupPageRest.CustomerGroupsData` (GET `/rs/customerGroups`). */
export interface CustomerGroupsData {
  groups?: CustomerGroupData[];
  businessUnits?: BusinessUnitData[];
  orderAccess?: OrderAccessData[];
  projectGroups?: ProjectGroupData[];
  /** Free text about the definition as a whole: how the groups and business units are drawn. */
  remark?: string | null;
  /** Epoch millis of the stored value, sent back on save for the optimistic lock. */
  lastUpdate?: number | null;
  /** The stored `ConfigurationDO` row, whose change history the page shows; null before the first save. */
  id?: number | null;
  /** The value as stored (JSON, ids only), the base of the diff (CustomerGroupsDiffButton). */
  storedJson?: string | null;
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

export interface OrderAccessValues extends ArrayRow {
  key: string;
  name: string;
  remark: string;
  users: EntityRef[];
  userGroups: EntityRef[];
  customers: EntityRef[];
  groups: string[];
  businessUnits: string[];
}

export interface ProjectGroupValues extends ArrayRow {
  key: string;
  name: string;
  projects: EntityRef[];
}

export interface CustomerGroupsValues {
  groups: CustomerSetValues[];
  businessUnits: BusinessUnitValues[];
  orderAccess: OrderAccessValues[];
  projectGroups: ProjectGroupValues[];
  remark: string;
  lastUpdate: number | null;
}
