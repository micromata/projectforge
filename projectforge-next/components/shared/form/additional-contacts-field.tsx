"use client";

import type { ComponentType } from "react";
import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";

/**
 * The further contact persons of an entity next to its main contact (`contactPerson`), bound to the form value
 * `additionalContacts` — an order's or a project's. All contacts are equal (access, notifications), the main
 * one is just named first.
 *
 * A custom field rather than a plain declaration: the entity keeps the list as comma separated user ids
 * (`AuftragDO`/`ProjektDO.additionalContactUserIds`), which is no field of the generated metadata a declared
 * name is checked against.
 */
export function AdditionalContactsField({
  hintKey,
  className,
}: {
  /** What being a contact means for this entity, e.g. `fibu.projekt.contacts.info`. */
  hintKey?: string;
  className?: string;
}) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="additionalContacts"
      label={t("additionalContacts")}
      hint={hintKey ? t(hintKey) : undefined}
      entity="user"
      metadataLess
      className={className}
    />
  );
}

/**
 * [AdditionalContactsField] with the entity's hint, for a section declaration (`{ custom: … }`), which hands
 * a custom field nothing but its class name.
 */
export function makeAdditionalContactsField(
  hintKey: string
): ComponentType<{ className?: string }> {
  function EntityAdditionalContactsField({
    className,
  }: {
    className?: string;
  }) {
    return <AdditionalContactsField hintKey={hintKey} className={className} />;
  }
  EntityAdditionalContactsField.displayName = `AdditionalContactsField(${hintKey})`;
  return EntityAdditionalContactsField;
}
