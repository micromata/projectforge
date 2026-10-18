import { z } from "zod";
import { ADDRESS_CAMPAIGN_VALUE_METADATA } from "@/lib/metadata/address-campaign-value.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Maximum lengths come from AddressCampaignValueDO through
 * `lib/metadata/address-campaign-value.generated.ts`. Which fields the form posts mirrors
 * org.projectforge.plugins.marketing.dto.AddressCampaignValue: beside the two editable ones the id, and for a
 * new value the address and the campaign it belongs to (`AddressCampaignValueEntityRest.transformForDB`).
 */
const m = fromMetadata(ADDRESS_CAMPAIGN_VALUE_METADATA);

export const addressCampaignValueSchema = z.object({
  id: z.number().nullable(),
  addressId: z.number().nullable(),
  addressCampaign: z.object({ id: z.number().nullable() }).nullable(),
  value: m.nullableString("value"),
  comment: m.nullableString("comment"),
});

export type AddressCampaignValueValues = z.infer<
  typeof addressCampaignValueSchema
>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const ADDRESS_CAMPAIGN_VALUE_FIELDS = ["value", "comment"] as const;
