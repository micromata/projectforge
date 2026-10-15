import { z } from "zod";
import type { EntityMetadata } from "@/lib/metadata/types";
import { i18nMarker, REQUIRED } from "@/lib/validation/markers";
import { isValidTextMember } from "./values";

/**
 * The metadata of a group's or business unit's fields. Hand-written: the configuration is JSON in one
 * configuration parameter, no entity, so there is nothing generated to read. Bound per row through
 * [NestedFieldMetadata], which strips the `groups[2].` prefix before the lookup.
 */
export const CUSTOMER_SET_METADATA: EntityMetadata = {
  entity: "CustomerGroup",
  historizable: false,
  fields: {
    name: { dataType: "STRING", required: true, maxLength: 255 },
    customers: { dataType: "CUSTOMIZED", required: false },
    texts: { dataType: "CUSTOMIZED", required: false },
    groups: { dataType: "CUSTOMIZED", required: false },
    tasks: { dataType: "CUSTOMIZED", required: false },
  },
};

/** The fields of the form level: only the remark, everything else lives in the rows. */
export const FORM_METADATA: EntityMetadata = {
  entity: "CustomerGroupConfig",
  historizable: false,
  fields: {
    remark: { dataType: "STRING", required: false },
  },
};

const entityRef = z.object({ id: z.number(), displayName: z.string() });

const customerSet = {
  key: z.string(),
  name: z.string().trim().min(1, REQUIRED).max(255),
  customers: z.array(entityRef),
  texts: z.array(z.string()).refine((texts) => texts.every(isValidTextMember), {
    message: i18nMarker("fibu.customerGroups.error.invalidText"),
  }),
};

/**
 * Anticipates the server's rules that need no other row (`CustomerGroupValidator`): names and the form
 * of the free texts. Duplicates, unknown customers and the membership rules are the server's (HTTP 406).
 */
export const customerGroupsSchema = z.object({
  groups: z.array(z.object(customerSet)),
  businessUnits: z.array(
    z.object({
      ...customerSet,
      groups: z.array(z.string()),
      tasks: z.array(entityRef),
    })
  ),
  remark: z.string(),
  lastUpdate: z.number().nullable(),
});

export const CUSTOMER_GROUPS_FIELDS = ["groups", "businessUnits"] as const;
