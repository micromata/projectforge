"use client";

import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { LICENSE_METADATA } from "@/lib/metadata/license.generated";
import { fetchLicenseSuggestions } from "@/lib/rs/license";

type SuggestedProperty = "organization" | "product";

/**
 * A free text completing from the values other licenses already have — Wicket's autocomplete fields of
 * `LicenseEditForm` (`LicenseDao.isAutocompletionPropertyEnabled` opts the two properties in).
 */
function LicenseSuggestField({
  property,
  className,
}: {
  property: SuggestedProperty;
  className?: string;
}) {
  const label = useFieldLabels(LICENSE_METADATA);
  return (
    <StringSuggestField
      name={property}
      label={label(property)}
      className={className}
      suggest={(search, signal) =>
        fetchLicenseSuggestions(property, search, signal)
      }
      // The completions depend on nothing but the property and the term the user is typing.
      queryKey={["license", "autocomplete", property]}
    />
  );
}

export function LicenseOrganizationField({
  className,
}: {
  className?: string;
}) {
  return <LicenseSuggestField property="organization" className={className} />;
}

export function LicenseProductField({ className }: { className?: string }) {
  return <LicenseSuggestField property="product" className={className} />;
}
