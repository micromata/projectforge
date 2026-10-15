"use client";

import { useTranslations } from "next-intl";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { leafKeyOf } from "@/lib/leaf-key";

/**
 * The DATEV account of the invoice ("11400 - Kreditoren").
 *
 * Custom because `KontoDO` has no `UIDataType`, so `ElementsRegistry` never reports it and the generated
 * metadata cannot carry it however the entity is annotated — hence `metadataLess`. The same field the
 * outgoing invoice has; both write `konto` by id.
 */
export function AccountField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityAutocompleteField
      name="konto"
      // `fibu.konto` is a text *and* the parent of the account's address block — see leafKeyOf.
      label={t(leafKeyOf("fibu.konto", t.has))}
      // The REST category of `KontoPagesRest` — `account`, not `konto` (see SEARCH_ENTITY).
      entity="account"
      metadataLess
      hint={t("fibu.rechnung.konto.tooltip")}
      className={className}
    />
  );
}
