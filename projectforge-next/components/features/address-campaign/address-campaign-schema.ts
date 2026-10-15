import { z } from "zod";
import { ADDRESS_CAMPAIGN_METADATA } from "@/lib/metadata/address-campaign.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { i18nMarker } from "@/lib/validation/markers";

/**
 * Mandatory and maximum length come from AddressCampaignDO through
 * `lib/metadata/address-campaign.generated.ts`. Which fields the form has mirrors
 * org.projectforge.plugins.marketing.dto.AddressCampaign.
 */
const m = fromMetadata(ADDRESS_CAMPAIGN_METADATA);

/**
 * Whether the text holds at least one value — the parse of `AddressCampaignDO.getValuesArray`
 * (split by semicolon, trimmed, blank entries dropped), which the backend checks on save as well
 * (`AddressCampaignEntityRest.validate`).
 */
function hasValues(values: string): boolean {
  return values.split(";").some((value) => value.trim().length > 0);
}

export const addressCampaignSchema = z.object({
  title: m.requiredString("title"),
  // A blank text is already reported as missing; only a given one is checked for its format.
  values: m
    .requiredString("values")
    .refine((v) => v.trim().length === 0 || hasValues(v), {
      message: i18nMarker(
        "plugins.marketing.addressCampaign.values.invalidFormat"
      ),
    }),
  comment: m.nullableString("comment"),
});

export type AddressCampaignValues = z.infer<typeof addressCampaignSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const ADDRESS_CAMPAIGN_FIELDS = Object.keys(
  addressCampaignSchema.shape
) as readonly (keyof AddressCampaignValues)[];
