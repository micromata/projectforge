"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";
import {
  NestedFieldMetadata,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { InputField } from "@/components/shared/form/input-field";
import { RepeatableList } from "@/components/shared/form/repeatable-list";
import { useFieldArray } from "@/hooks/use-field-array";
import { CustomerSetRow } from "./customer-set-row";
import { PROJECT_GROUP_METADATA } from "./schema";
import type { CustomerGroupsValues, ProjectGroupValues } from "./types";
import { newKey, usedKeys } from "./values";

/**
 * The project groups: projects belonging together under a common parent project, offered as one entry
 * in the project filters of the lists. Each project belongs to at most one group.
 */
export function ProjectGroupList() {
  const t = useTranslations();
  const form = useEntityEditForm();
  const array = useFieldArray<ProjectGroupValues>("projectGroups");
  // Read on adding rather than subscribed to: the keys only matter at that moment.
  const values = () => form.state.values as CustomerGroupsValues;
  // The rows added in this edit, by key: they open, the stored ones stay folded.
  const [added, setAdded] = useState<ReadonlySet<string>>(new Set());
  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">
        {t("fibu.projectGroups.intro")}
      </p>
      <RepeatableList
        array={array}
        emptyText={t("fibu.projectGroups.empty")}
        addLabel={t("fibu.projectGroups.add")}
        onAdd={() => {
          const key = newKey(usedKeys(values()));
          setAdded((keys) => new Set(keys).add(key));
          array.add({ key, name: "", projects: [] });
        }}
        row={(group, index) => {
          const prefix = array.fieldName(index, "");
          return (
            <NestedFieldMetadata
              metadata={PROJECT_GROUP_METADATA}
              namePrefix={prefix}
            >
              <CustomerSetRow
                prefix={prefix}
                set={{ name: group.name, customers: group.projects, texts: [] }}
                isNew={added.has(group.key)}
                onRemove={() => array.remove(index)}
                removeLabel={t("fibu.projectGroups.remove", {
                  arg0: group.name,
                })}
              >
                <InputField
                  name={`${prefix}name`}
                  label={t("fibu.customerGroups.name")}
                />
                <EntityMultiAutocompleteField
                  name={`${prefix}projects`}
                  label={t("fibu.projectGroups.projects")}
                  entity="project"
                  sorted
                  className="md:col-span-2"
                />
              </CustomerSetRow>
            </NestedFieldMetadata>
          );
        }}
      />
    </div>
  );
}
