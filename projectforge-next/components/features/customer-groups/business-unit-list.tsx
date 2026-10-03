"use client";

import { useState } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import {
  NestedFieldMetadata,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";
import { RepeatableList } from "@/components/shared/form/repeatable-list";
import { useFieldArray } from "@/hooks/use-field-array";
import { CUSTOMER_SET_METADATA } from "./schema";
import { CustomerSetFields } from "./customer-set-fields";
import { CustomerSetRow } from "./customer-set-row";
import { GroupsSelectField } from "./groups-select-field";
import type { BusinessUnitValues, CustomerGroupsValues } from "./types";
import { newKey, usedKeys } from "./values";

/** The business units: customer groups and single customers combined under one name. */
export function BusinessUnitList() {
  const t = useTranslations();
  const form = useEntityEditForm();
  const array = useFieldArray<BusinessUnitValues>("businessUnits");
  // Read on adding rather than subscribed to: the keys only matter at that moment.
  const values = () => form.state.values as CustomerGroupsValues;
  // The rows added in this edit, by key: they open, the stored ones stay folded.
  const [added, setAdded] = useState<ReadonlySet<string>>(new Set());
  // The groups' names for the headers, which list a business unit's groups by name.
  const groups = useStore(
    form.store,
    (s: unknown) => (s as { values: CustomerGroupsValues }).values.groups
  );
  const groupName = new Map(groups.map((g) => [g.key, g.name.trim()]));
  return (
    <RepeatableList
      array={array}
      emptyText={t("fibu.businessUnits.empty")}
      addLabel={t("fibu.businessUnits.add")}
      onAdd={() => {
        const key = newKey(usedKeys(values()));
        setAdded((keys) => new Set(keys).add(key));
        array.add({
          key,
          name: "",
          groups: [],
          tasks: [],
          customers: [],
          texts: [],
        });
      }}
      row={(bu, index) => {
        const prefix = array.fieldName(index, "");
        return (
          <NestedFieldMetadata
            metadata={CUSTOMER_SET_METADATA}
            namePrefix={prefix}
          >
            <CustomerSetRow
              prefix={prefix}
              set={bu}
              groupNames={bu.groups.flatMap((key) => groupName.get(key) || [])}
              tasks={bu.tasks}
              isNew={added.has(bu.key)}
              onRemove={() => array.remove(index)}
              removeLabel={t("fibu.businessUnits.remove", { arg0: bu.name })}
            >
              <CustomerSetFields prefix={prefix} />
              <GroupsSelectField
                name={`${prefix}groups`}
                label={t("fibu.businessUnits.groups")}
                className="md:col-span-2"
              />
              <EntityMultiAutocompleteField
                name={`${prefix}tasks`}
                entity="task/tree"
                label={t("fibu.businessUnits.tasks")}
                hint={t("fibu.businessUnits.tasksHint")}
                sorted
                className="md:col-span-2"
              />
            </CustomerSetRow>
          </NestedFieldMetadata>
        );
      }}
    />
  );
}
