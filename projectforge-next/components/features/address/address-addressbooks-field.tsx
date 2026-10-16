"use client";

import { leafKeyOf } from "@/lib/leaf-key";
import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";
import { cn } from "@/lib/utils";

/**
 * The address books an address is in, and whether it is one of the logged-in user's favorites — side by side,
 * as the former form had them. The books are searched among the ones the user may pick
 * (`addressBook/autosearch`); at least one is required, which the backend checks (`AddressSaveSupport`).
 *
 * Custom because neither is a field of AddressDO's metadata: the books are a collection, the favorite is the
 * user's own flag (a `PersonalAddressDO`), not the address's.
 */
export function AddressAddressbooksField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  return (
    <div
      className={cn(
        "grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-end",
        className
      )}
    >
      <EntityMultiAutocompleteField
        name="addressbookList"
        label={t("address.addressbooks")}
        entity="addressBook"
        metadataLess
      />
      <CheckboxField
        name="isFavoriteCard"
        label={t(leafKeyOf("favorite", t.has))}
        hint={t("address.favorites.info")}
        className="sm:pb-2"
      />
    </div>
  );
}
