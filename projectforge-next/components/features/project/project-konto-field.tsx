"use client";

import { useTranslations } from "next-intl";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { leafKeyOf } from "@/lib/leaf-key";

/**
 * The DATEV account of the project, used by the invoice export in place of the customer's.
 *
 * Custom for the reason the customer's account is (see CustomerKontoField): `KontoDO` has no
 * `UIDataType`, so the generated metadata cannot carry it — hence `metadataLess`.
 */
export function ProjectKontoField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityAutocompleteField
      name="konto"
      // `fibu.konto` is a text *and* the parent of the account's block — see leafKeyOf.
      label={t(leafKeyOf("fibu.konto", t.has))}
      // The REST category of `KontoPagesRest` is `account`, not `konto`.
      entity="account"
      metadataLess
      hint={t("fibu.projekt.konto.tooltip")}
      className={className}
    />
  );
}
