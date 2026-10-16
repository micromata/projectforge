"use client";

import { StringSuggestField } from "@/components/shared/form/string-suggest-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { ADDRESS_METADATA } from "@/lib/metadata/address.generated";
import { fetchAddressSuggestions } from "@/lib/rs/address";

/** The fields AddressDao completes from the values of other addresses (`isAutocompletionPropertyEnabled`). */
type SuggestedProperty =
  | "organization"
  | "addressText"
  | "addressText2"
  | "postalAddressText"
  | "postalAddressText2"
  | "privateAddressText"
  | "privateAddressText2";

function AddressSuggestField({
  property,
  className,
}: {
  property: SuggestedProperty;
  className?: string;
}) {
  const label = useFieldLabels(ADDRESS_METADATA);
  return (
    <StringSuggestField
      name={property}
      label={label(property)}
      className={className}
      suggest={(search, signal) =>
        fetchAddressSuggestions(property, search, signal)
      }
      // The completions depend on nothing but the property and the term the user is typing.
      queryKey={["address", "autocomplete", property]}
    />
  );
}

type FieldProps = { className?: string };

export function AddressOrganizationField({ className }: FieldProps) {
  return <AddressSuggestField property="organization" className={className} />;
}

export function AddressTextField({ className }: FieldProps) {
  return <AddressSuggestField property="addressText" className={className} />;
}

export function AddressText2Field({ className }: FieldProps) {
  return <AddressSuggestField property="addressText2" className={className} />;
}

export function PostalAddressTextField({ className }: FieldProps) {
  return (
    <AddressSuggestField property="postalAddressText" className={className} />
  );
}

export function PostalAddressText2Field({ className }: FieldProps) {
  return (
    <AddressSuggestField property="postalAddressText2" className={className} />
  );
}

export function PrivateAddressTextField({ className }: FieldProps) {
  return (
    <AddressSuggestField property="privateAddressText" className={className} />
  );
}

export function PrivateAddressText2Field({ className }: FieldProps) {
  return (
    <AddressSuggestField property="privateAddressText2" className={className} />
  );
}
