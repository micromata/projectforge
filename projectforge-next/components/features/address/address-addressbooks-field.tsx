"use client";

import { leafKeyOf } from "@/lib/leaf-key";
import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";
import { cn } from "@/lib/utils";

/**
 * The address books an address is in. They are searched among the ones the user may pick
 * (`addressBook/autosearch`); at least one is required, which the backend checks (`AddressSaveSupport`).
 *
 * Custom because it is no field of AddressDO's metadata, but a collection.
 */
export function AddressAddressbooksField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="addressbookList"
      label={t("address.addressbooks")}
      entity="addressBook"
      metadataLess
      className={className}
    />
  );
}

/**
 * Whether the address is one of the logged-in user's favorites — the user's own flag (a `PersonalAddressDO`),
 * not the address's. At the right end of the first row, beside the two states.
 */
export function AddressFavoriteField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <CheckboxField
      name="isFavoriteCard"
      label={t(leafKeyOf("favorite", t.has))}
      hint={t("address.favorites.info")}
      className={cn(
        "md:w-auto md:justify-self-end md:self-end md:pb-2",
        className
      )}
    />
  );
}
