"use client";

import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";

/**
 * The users and groups with access to a bank account and its transactions — each a multi select bound to the DTO's
 * list of references.
 *
 * Custom fields because none of them is a field of BankAccountDO's metadata: the DO stores csv lists of ids
 * (`fullAccessUserIds`, …), which is what a declared name is checked against.
 */
function RefsField({
  name,
  entity,
  labelKey,
  className,
}: {
  name: string;
  entity: "user" | "group";
  labelKey: string;
  className?: string;
}) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name={name}
      label={t(labelKey)}
      entity={entity}
      metadataLess
      className={className}
    />
  );
}

type FieldProps = { className?: string };

export function BankAccountFullAccessUsersField({ className }: FieldProps) {
  return (
    <RefsField
      name="fullAccessUsers"
      entity="user"
      labelKey="plugins.banking.account.fullAccessUsers"
      className={className}
    />
  );
}

export function BankAccountFullAccessGroupsField({ className }: FieldProps) {
  return (
    <RefsField
      name="fullAccessGroups"
      entity="group"
      labelKey="plugins.banking.account.fullAccessGroups"
      className={className}
    />
  );
}

export function BankAccountReadonlyAccessUsersField({ className }: FieldProps) {
  return (
    <RefsField
      name="readonlyAccessUsers"
      entity="user"
      labelKey="plugins.banking.account.readonlyAccessUsers"
      className={className}
    />
  );
}

export function BankAccountReadonlyAccessGroupsField({
  className,
}: FieldProps) {
  return (
    <RefsField
      name="readonlyAccessGroups"
      entity="group"
      labelKey="plugins.banking.account.readonlyAccessGroups"
      className={className}
    />
  );
}
