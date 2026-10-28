"use client";

import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";

/**
 * The contact persons of the visitor, employees, bound to `Visitorbook.contactPersons`. A custom field because a
 * collection is no field of VisitorbookDO's metadata, which is what a declared name is checked against.
 */
export function VisitorbookContactPersonsField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="contactPersons"
      label={t("orga.visitorbook.contactPersons")}
      entity="employee"
      metadataLess
      className={className}
    />
  );
}
