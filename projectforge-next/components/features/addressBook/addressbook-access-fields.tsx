"use client";

import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";

/**
 * The users and groups with access to the addresses of an address book — each a multi select bound to the DTO's
 * list of references.
 *
 * Custom fields because none of them is a field of AddressbookDO's metadata: the DO stores csv lists of ids
 * (`fullAccessUserIds`, …), which is what a declared name is checked against.
 */
function RefsField({
  name,
  entity,
  label,
  hint,
  className,
}: {
  name: string;
  entity: "user" | "group";
  label: string;
  hint?: string;
  className?: string;
}) {
  return (
    <EntityMultiAutocompleteField
      name={name}
      label={label}
      hint={hint}
      entity={entity}
      metadataLess
      className={className}
    />
  );
}

type FieldProps = { className?: string };

export function AddressbookFullAccessUsersField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <RefsField
      name="fullAccessUsers"
      entity="user"
      label={t("addressbook.fullAccess.users")}
      hint={t("addressbook.fullAccess.tooltip")}
      className={className}
    />
  );
}

export function AddressbookFullAccessGroupsField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <RefsField
      name="fullAccessGroups"
      entity="group"
      label={t("addressbook.fullAccess.groups")}
      hint={t("addressbook.fullAccess.tooltip")}
      className={className}
    />
  );
}

export function AddressbookReadonlyAccessUsersField({ className }: FieldProps) {
  const t = useTranslations();
  return (
    <RefsField
      name="readonlyAccessUsers"
      entity="user"
      label={t("addressbook.readonlyAccess.users")}
      hint={t("addressbook.readonlyAccess.tooltip")}
      className={className}
    />
  );
}

export function AddressbookReadonlyAccessGroupsField({
  className,
}: FieldProps) {
  const t = useTranslations();
  return (
    <RefsField
      name="readonlyAccessGroups"
      entity="group"
      label={t("addressbook.readonlyAccess.groups")}
      hint={t("addressbook.readonlyAccess.tooltip")}
      className={className}
    />
  );
}
