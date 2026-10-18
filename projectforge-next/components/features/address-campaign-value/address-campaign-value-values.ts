import type { AddressCampaignValueValues } from "./address-campaign-value-schema";
import type { AddressCampaignValueDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(
  entry: AddressCampaignValueDetail
): AddressCampaignValueValues {
  return {
    id: entry.id ?? null,
    addressId: entry.addressId ?? null,
    addressCampaign:
      entry.addressCampaign?.id != null
        ? { id: entry.addressCampaign.id }
        : null,
    value: entry.value ?? null,
    comment: entry.comment ?? null,
  };
}

/** Blank form; the edit page fetches the preset of the address and the campaign from `newEntry`. */
export function emptyAddressCampaignValueValues(): AddressCampaignValueValues {
  return toFormValues({ id: null });
}
