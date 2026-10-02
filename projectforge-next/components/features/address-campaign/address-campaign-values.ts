import type { AddressCampaignValues } from "./address-campaign-schema";
import type { AddressCampaignDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(
  campaign: AddressCampaignDetail
): AddressCampaignValues {
  return {
    title: campaign.title ?? "",
    values: campaign.values ?? "",
    comment: campaign.comment ?? null,
  };
}

/** Blank form for a campaign that doesn't exist yet. */
export function emptyAddressCampaignValues(): AddressCampaignValues {
  return toFormValues({ id: null });
}
