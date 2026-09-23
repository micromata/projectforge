"use client";

import { useTranslations } from "next-intl";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { leafKeyOf } from "@/lib/leaf-key";

/**
 * The DATEV account of the customer ("11400 - Debitoren").
 *
 * Custom for the reason the invoice's account is: `KontoDO` has no `UIDataType`, so `ElementsRegistry`
 * never reports it and the generated metadata cannot carry it however the entity is annotated — hence
 * `metadataLess`. Wicket restricts the picker to the debtor account ranges and hides the field when no
 * account exists; neither is done here — `/rs/account/autosearch` searches all of them, and an
 * installation without accounts simply finds nothing (same accepted limitation as the invoice's field).
 */
export function CustomerKontoField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityAutocompleteField
      name="konto"
      // `fibu.konto` is a text *and* the parent of the account's block — see leafKeyOf.
      label={t(leafKeyOf("fibu.konto", t.has))}
      // The REST category of `KontoPagesRest` is `account`, not `konto`.
      entity="account"
      metadataLess
      hint={t("fibu.kunde.konto.tooltip")}
      className={className}
    />
  );
}
