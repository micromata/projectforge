"use client";

import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";

/**
 * The further contact persons of an order next to its main contact (`contactPerson`), bound to
 * `Auftrag.additionalContacts`. They have the main contact's access to the order and get its change mail.
 *
 * A custom field rather than a plain declaration: the entity keeps the list as comma separated user ids
 * (`AuftragDO.additionalContactUserIds`), which is no field of the generated metadata a declared name is
 * checked against.
 */
export function AdditionalContactsField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="additionalContacts"
      label={t("fibu.auftrag.additionalContacts")}
      hint={t("fibu.auftrag.contacts.info")}
      entity="user"
      metadataLess
      className={className}
    />
  );
}
