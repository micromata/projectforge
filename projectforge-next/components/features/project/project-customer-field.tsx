"use client";

import { useTranslations } from "next-intl";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { leafKeyOf } from "@/lib/leaf-key";
import { useNumberLocked } from "./project-number-field";

/**
 * The customer of the project. Its number is the range (Bereich) of the project's cost number, so
 * picking or clearing one re-shapes ProjectNumberField. `KundeDO` has no `UIDataType`, so it carries no
 * metadata — hence `metadataLess`, as for the account. Fixed together with the number (useNumberLocked).
 */
export function ProjectCustomerField({ className }: { className?: string }) {
  const t = useTranslations();
  const locked = useNumberLocked();
  return (
    <EntityAutocompleteField
      name="customer"
      // `fibu.kunde` is a text *and* the parent of the customer's block — see leafKeyOf.
      label={t(leafKeyOf("fibu.kunde", t.has))}
      entity="customer"
      metadataLess
      disabled={locked}
      hint={locked ? t("fibu.projekt.validation.numberLocked") : undefined}
      className={className}
    />
  );
}
