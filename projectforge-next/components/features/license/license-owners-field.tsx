"use client";

import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";

/**
 * The owners of the license — Wicket's user multi select (`UsersComparator` / `Select2MultiChoice`),
 * bound to `License.owners`. An owner may see the license's key and files.
 *
 * A custom field because `owners` is no field of LicenseDO's metadata (the DO stores a csv of ids,
 * `ownerIds`), which is what a declared name is checked against.
 */
export function LicenseOwnersField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="owners"
      label={t("plugins.licensemanagement.owner")}
      entity="user"
      metadataLess
      className={className}
    />
  );
}
