"use client";

import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";
import {
  NestedFieldMetadata,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { InputField } from "@/components/shared/form/input-field";
import { RepeatableList } from "@/components/shared/form/repeatable-list";
import { useFieldArray } from "@/hooks/use-field-array";
import {
  fetchOrderAccessInfo,
  type IneffectiveGrantee,
} from "@/lib/rs/customer-groups";
import { CustomerSetRow } from "./customer-set-row";
import { GroupsSelectField } from "./groups-select-field";
import { ORDER_ACCESS_METADATA } from "./schema";
import type { CustomerGroupsValues, OrderAccessValues } from "./types";
import { useDebouncedPayload } from "./use-debounced-payload";
import { newKey, usedKeys } from "./values";
import { RefName } from "@/components/shared/ref-list";

/**
 * The order access grants: read access to all orders of customers, customer groups and business units
 * (with their invoices, forecast and contribution margins) for users and the members of groups, as far
 * as they are project managers or assistants with access to the order book.
 */
export function OrderAccessList() {
  const t = useTranslations();
  const form = useEntityEditForm();
  const array = useFieldArray<OrderAccessValues>("orderAccess");
  // Read on adding rather than subscribed to: the keys only matter at that moment.
  const values = () => form.state.values as CustomerGroupsValues;
  // The rows added in this edit, by key: they open, the stored ones stay folded.
  const [added, setAdded] = useState<ReadonlySet<string>>(new Set());
  // The names of the groups and business units for the headers, which list them by name.
  const { groups, businessUnits } = useStore(
    form.store,
    (s: unknown) => (s as { values: CustomerGroupsValues }).values
  );
  const groupName = new Map(groups.map((g) => [g.key, g.name.trim()]));
  const businessUnitName = new Map(
    businessUnits.map((bu) => [bu.key, bu.name.trim()])
  );
  // The users a grant has no effect for, asked once for all of them: only on changes of the grants, and
  // not at all without any.
  const grants = useDebouncedPayload().orderAccess;
  const { data: ineffective } = useQuery({
    queryKey: ["customerGroups", "orderAccessInfo", grants],
    queryFn: ({ signal }) => fetchOrderAccessInfo(grants, signal),
    placeholderData: keepPreviousData,
    enabled: grants.length > 0,
  });
  return (
    <div className="flex flex-col gap-3">
      <p className="text-sm text-muted-foreground">
        {t("fibu.customerGroups.orderAccess.intro")}
      </p>
      <RepeatableList
        array={array}
        emptyText={t("fibu.customerGroups.orderAccess.empty")}
        addLabel={t("fibu.customerGroups.orderAccess.add")}
        onAdd={() => {
          const key = newKey(usedKeys(values()));
          setAdded((keys) => new Set(keys).add(key));
          array.add({
            key,
            name: "",
            remark: "",
            users: [],
            userGroups: [],
            customers: [],
            groups: [],
            businessUnits: [],
          });
        }}
        row={(grant, index) => {
          const prefix = array.fieldName(index, "");
          return (
            <NestedFieldMetadata
              metadata={ORDER_ACCESS_METADATA}
              namePrefix={prefix}
            >
              <CustomerSetRow
                prefix={prefix}
                set={{ ...grant, texts: [] }}
                leading={[...grant.users, ...grant.userGroups].map((ref) => (
                  <Badge
                    key={`u-${ref.id}-${ref.displayName}`}
                    variant="outline"
                    className="font-normal"
                  >
                    <RefName value={ref} />
                  </Badge>
                ))}
                groupNames={[
                  ...grant.groups.flatMap((key) => groupName.get(key) || []),
                  ...grant.businessUnits.flatMap(
                    (key) => businessUnitName.get(key) || []
                  ),
                ]}
                isNew={added.has(grant.key)}
                onRemove={() => array.remove(index)}
                removeLabel={t("fibu.customerGroups.orderAccess.remove", {
                  arg0: grant.name,
                })}
              >
                <InputField
                  name={`${prefix}name`}
                  label={t("fibu.customerGroups.name")}
                />
                <InputField name={`${prefix}remark`} label={t("comment")} />
                <EntityMultiAutocompleteField
                  name={`${prefix}users`}
                  label={t("fibu.customerGroups.orderAccess.users")}
                  entity="user"
                  metadataLess
                />
                <EntityMultiAutocompleteField
                  name={`${prefix}userGroups`}
                  label={t("fibu.customerGroups.orderAccess.userGroups")}
                  entity="group"
                  metadataLess
                />
                <EntityMultiAutocompleteField
                  name={`${prefix}customers`}
                  label={t("fibu.customerGroups.customers")}
                  entity="customer"
                  sorted
                  className="md:col-span-2"
                />
                <GroupsSelectField
                  name={`${prefix}groups`}
                  label={t("fibu.customerGroups.groups")}
                />
                <GroupsSelectField
                  name={`${prefix}businessUnits`}
                  label={t("fibu.businessUnits.title")}
                  source="businessUnits"
                />
                <IneffectiveGrantees grantees={ineffective?.[grant.key]} />
              </CustomerSetRow>
            </NestedFieldMetadata>
          );
        }}
      />
    </div>
  );
}

/** The users named by a grant it has no effect for, with the reason; nothing if there are none. */
function IneffectiveGrantees({
  grantees,
}: {
  grantees: IneffectiveGrantee[] | undefined;
}) {
  const t = useTranslations();
  if (!grantees?.length) return null;
  const reasons: Record<IneffectiveGrantee["reason"], string> = {
    DEACTIVATED: t("fibu.customerGroups.orderAccess.ineffective.deactivated"),
    NO_PROJECT_MANAGER: t(
      "fibu.customerGroups.orderAccess.ineffective.noProjectManager"
    ),
    NO_ORDER_BOOK_RIGHT: t(
      "fibu.customerGroups.orderAccess.ineffective.noOrderBookRight"
    ),
  };
  return (
    <div className="flex flex-col gap-1 text-xs md:col-span-2">
      <h3 className="font-semibold">
        {t("fibu.customerGroups.orderAccess.ineffective._", {
          arg0: grantees.length,
        })}
      </h3>
      <ul className="grid gap-x-4 sm:grid-cols-2">
        {grantees.map((grantee) => (
          <li
            key={`${grantee.name}:${grantee.reason}`}
            className="flex min-w-0 gap-1.5"
          >
            <span className="truncate">{grantee.name}</span>
            <span className="text-muted-foreground">
              {reasons[grantee.reason]}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
