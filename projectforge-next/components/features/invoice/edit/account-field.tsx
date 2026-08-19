"use client";

import { useTranslations } from "next-intl";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { leafKeyOf } from "@/lib/leaf-key";

/**
 * The DATEV account of the invoice ("11400 - Debitoren").
 *
 * Custom for the same reason the customer and the project are: `KontoDO` has no `UIDataType`, so
 * `ElementsRegistry` never reports it and the generated metadata cannot carry it however the entity is
 * annotated — hence `metadataLess`.
 *
 * Wicket restricts the picker to `AccountingConfig.debitorsAccountNumberRanges` and hides the field
 * entirely when no account exists. Neither is done here: `/rs/account/autosearch` searches all of them,
 * and an installation without accounts simply finds nothing — a range filter would need an endpoint that
 * knows about it.
 */
export function AccountField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityAutocompleteField
      name="konto"
      // `fibu.konto` is a text *and* the parent of the whole address block — see leafKeyOf.
      label={t(leafKeyOf("fibu.konto", t.has))}
      // The REST category of `KontoPagesRest` — `account`, not `konto` (see SEARCH_ENTITY).
      entity="account"
      metadataLess
      hint={t("fibu.rechnung.konto.tooltip")}
      className={className}
    />
  );
}
