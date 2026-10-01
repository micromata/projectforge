import { ADDRESS_CAMPAIGN_METADATA } from "@/lib/metadata/address-campaign.generated";
import { definePage } from "@/lib/page-def/define-page";
import {
  addressCampaignSchema,
  ADDRESS_CAMPAIGN_FIELDS,
  type AddressCampaignValues,
} from "./address-campaign-schema";
import { AddressCampaignValuesField } from "./address-campaign-values-field";
import {
  emptyAddressCampaignValues,
  toFormValues,
} from "./address-campaign-values";
import type { AddressCampaignDetail, AddressCampaignListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const ADDRESS_CAMPAIGN_LIST_QUERY_KEY = ["addressCampaign"] as const;

/**
 * The whole address campaign page of the marketing plugin — list and edit — as data (see
 * lib/page-def/types.ts).
 *
 * Replaces the server-laid-out React page (`AddressCampaignPagesRest`) and follows the legacy Wicket
 * pages, which stay reachable as the classic version: the list shows `AddressCampaignListPage`'s columns
 * in its order, sorted by title as it does; the form has `AddressCampaignEditForm`'s fields, including the
 * values' format hint and the warning against relabelling them. Labels and every rule come from
 * AddressCampaignDO through the generated metadata.
 */
export const ADDRESS_CAMPAIGN_PAGE = definePage<
  AddressCampaignListRow,
  AddressCampaignValues,
  AddressCampaignDetail,
  typeof ADDRESS_CAMPAIGN_METADATA
>({
  entity: "addressCampaign",
  metadata: ADDRESS_CAMPAIGN_METADATA,
  route: "/address-campaign",
  queryKey: ADDRESS_CAMPAIGN_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Misc (MarketingPlugin, MenuItemDefId.MISC).
  categoryKey: "menu.misc",
  titleKey: "plugins.marketing.addressCampaign.title.list",
  defaultSort: { id: "title" },
  columns: [
    { name: "created", size: 130 },
    { name: "lastUpdate", size: 130 },
    { name: "title", size: 260, className: "font-semibold" },
    { name: "values", size: 320 },
    { name: "comment", size: 400 },
  ],
  edit: {
    schema: addressCampaignSchema,
    fieldNames: ADDRESS_CAMPAIGN_FIELDS,
    defaultValues: emptyAddressCampaignValues,
    toFormValues,
    title: (campaign) => campaign.title ?? "",
    newTitleKey: "plugins.marketing.addressCampaign.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "general",
        titleKey: "plugins.marketing.addressCampaign",
        fields: [
          { name: "title", span: 3, emphasized: true },
          { custom: AddressCampaignValuesField, span: 3 },
          { name: "comment", span: 3, rows: 4 },
        ],
      },
    ],
  },
});
