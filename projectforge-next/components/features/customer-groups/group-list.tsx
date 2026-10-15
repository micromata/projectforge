"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  NestedFieldMetadata,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { RepeatableList } from "@/components/shared/form/repeatable-list";
import { useFieldArray } from "@/hooks/use-field-array";
import { CUSTOMER_SET_METADATA } from "./schema";
import { CustomerSetFields } from "./customer-set-fields";
import { CustomerSetRow } from "./customer-set-row";
import type { CustomerGroupsValues, CustomerSetValues } from "./types";
import { newKey, usedKeys } from "./values";

/** The customer groups: a name, and the customers and free texts that make up the group. */
export function GroupList() {
  const t = useTranslations();
  const form = useEntityEditForm();
  const array = useFieldArray<CustomerSetValues>("groups");
  // Read on adding rather than subscribed to: the keys only matter at that moment.
  const values = () => form.state.values as CustomerGroupsValues;
  // The rows added in this edit, by key: they open, the stored ones stay folded.
  const [added, setAdded] = useState<ReadonlySet<string>>(new Set());
  return (
    <RepeatableList
      array={array}
      emptyText={t("fibu.customerGroups.empty")}
      addLabel={t("fibu.customerGroups.add")}
      onAdd={() => {
        const key = newKey(usedKeys(values()));
        setAdded((keys) => new Set(keys).add(key));
        array.add({ key, name: "", customers: [], texts: [] });
      }}
      row={(group, index) => {
        const prefix = array.fieldName(index, "");
        return (
          <NestedFieldMetadata
            metadata={CUSTOMER_SET_METADATA}
            namePrefix={prefix}
          >
            <CustomerSetRow
              prefix={prefix}
              set={group}
              isNew={added.has(group.key)}
              onRemove={() => array.remove(index)}
              removeLabel={t("fibu.customerGroups.remove", {
                arg0: group.name,
              })}
            >
              <CustomerSetFields prefix={prefix} />
            </CustomerSetRow>
          </NestedFieldMetadata>
        );
      }}
    />
  );
}
