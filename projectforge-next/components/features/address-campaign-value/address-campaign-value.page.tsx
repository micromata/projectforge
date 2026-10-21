import { ADDRESS_CAMPAIGN_VALUE_METADATA } from "@/lib/metadata/address-campaign-value.generated";
import { definePage } from "@/lib/page-def/define-page";
import {
  addressCampaignValueSchema,
  ADDRESS_CAMPAIGN_VALUE_FIELDS,
  type AddressCampaignValueValues,
} from "./address-campaign-value-schema";
import {
  emptyAddressCampaignValueValues,
  toFormValues,
} from "./address-campaign-value-values";
import { AddressCampaignValueAddressCell } from "./address-campaign-value-address-cell";
import { AddressCampaignValueCampaignLine } from "./address-campaign-value-campaign-line";
import { AddressCampaignValueEditIntro } from "./address-campaign-value-edit-intro";
import { AddressCampaignValueListActions } from "./address-campaign-value-list-actions";
import { AddressCampaignValueSelectField } from "./address-campaign-value-select-field";
import type {
  AddressCampaignValueDetail,
  AddressCampaignValueListRow,
  AddressCampaignValueStatistics,
} from "./types";
import { isSyntheticRow } from "./types";

/** REST category — `AddressCampaignValueEntityRest` is mapped to "addressCampaignValue". */
export const ADDRESS_CAMPAIGN_VALUE_ENTITY = "addressCampaignValue";
/** React Query key of the list, so a write from the edit page refreshes it. */
export const ADDRESS_CAMPAIGN_VALUE_LIST_QUERY_KEY = [
  "addressCampaignValue",
] as const;
/**
 * The tint of the comparison campaign's columns: a neutral grey, apart from the teal of the favorite rows.
 * Mixed opaque rather than translucent, so a column the user pins still covers the ones scrolling beneath, and
 * as the colour rather than an image, so a row colour (an image, see globals.css) still lies over it.
 */
const COMPARISON_CELL_CLASS =
  "bg-[color-mix(in_oklab,var(--muted)_60%,var(--background))]";
/** Route of the list; the edit page hangs below it as `/address-campaign-value/{id}`. */
export const ADDRESS_CAMPAIGN_VALUE_ROUTE = "/address-campaign-value";

/**
 * The values of the address campaigns (marketing plugin) — list, form and mass update — as data (see
 * lib/page-def/types.ts).
 *
 * Replaces the server-laid-out React page (`AddressCampaignValuePagesRest`), which it follows: one row per
 * address of the campaign selected in the campaign filter, with the value and comment the address has in it.
 * An address without a value yet is a row as well (synthetic negative id); its click opens the form for a new
 * value of that address in that campaign. The React page is gone, there is no way back.
 *
 * Every column is sorted by the backend in memory (`AddressCampaignValueEntityRest.computedSortProperties`):
 * the query runs on the addresses.
 */
export const ADDRESS_CAMPAIGN_VALUE_PAGE = definePage<
  AddressCampaignValueListRow,
  AddressCampaignValueValues,
  AddressCampaignValueDetail,
  typeof ADDRESS_CAMPAIGN_VALUE_METADATA
>({
  entity: ADDRESS_CAMPAIGN_VALUE_ENTITY,
  metadata: ADDRESS_CAMPAIGN_VALUE_METADATA,
  route: ADDRESS_CAMPAIGN_VALUE_ROUTE,
  queryKey: ADDRESS_CAMPAIGN_VALUE_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Misc (MarketingPlugin, MenuItemDefId.MISC).
  categoryKey: "menu.misc",
  titleKey: "plugins.marketing.addressCampaignValue.title.list",
  defaultSort: { id: "name" },
  columns: [
    {
      id: "name",
      labelKey: "contact.name",
      accessor: (row) => row.name ?? null,
      size: 160,
      pinned: "left",
    },
    {
      id: "firstName",
      labelKey: "contact.firstname",
      accessor: (row) => row.firstName ?? null,
      size: 140,
      pinned: "left",
    },
    { name: "value", size: 140, className: "font-semibold" },
    // The value of the comparison campaign (e.g. last year's), only while one is selected in the filter. Both
    // comparison columns are tinted to set them apart from the campaign's own; a comparison value that differs
    // from the row's value stands out — what is still to be decided or adopted.
    {
      id: "comparisonValue",
      labelKey: "plugins.marketing.addressCampaignValue.comparisonValue",
      accessor: (row) => row.comparisonValue ?? null,
      size: 140,
      sortable: false,
      revealedByFilter: "comparisonCampaignId",
      cellClassName: COMPARISON_CELL_CLASS,
      cell: ({ row }) => {
        const { comparisonValue, value } = row.original;
        if (!comparisonValue) return null;
        return comparisonValue === (value || null) ? (
          <span className="text-muted-foreground">{comparisonValue}</span>
        ) : (
          <span className="font-semibold text-brand-pink">
            {comparisonValue}
          </span>
        );
      },
    },
    {
      id: "organization",
      labelKey: "organization",
      accessor: (row) => row.organization ?? null,
      size: 200,
      wrap: true,
      cell: ({ row, table }) => (
        <AddressCampaignValueAddressCell
          addressId={row.original.addressId}
          selecting={!!table.options.enableRowSelection}
        >
          {row.original.organization}
        </AddressCampaignValueAddressCell>
      ),
    },
    {
      id: "formattedAddress",
      labelKey: "address",
      accessor: (row) => row.formattedAddress ?? null,
      size: 240,
      wrap: true,
      sortable: false,
      cell: ({ row, table }) => (
        <AddressCampaignValueAddressCell
          addressId={row.original.addressId}
          multiline
          selecting={!!table.options.enableRowSelection}
        >
          {row.original.formattedAddress}
        </AddressCampaignValueAddressCell>
      ),
    },
    { name: "lastUpdate", size: 130 },
    {
      id: "lastUpdateOfAddress",
      labelKey: "plugins.marketing.addressCampaign.lastUpdateOfAddress",
      accessor: (row) => row.lastUpdateOfAddress ?? null,
      dataType: "TIMESTAMP",
      size: 130,
    },
    {
      id: "contactStatus",
      labelKey: "address.contactStatus",
      accessor: (row) => row.contactStatusAsString ?? null,
      size: 110,
    },
    {
      id: "addressStatus",
      labelKey: "address.addressStatus",
      accessor: (row) => row.addressStatusAsString ?? null,
      size: 110,
    },
    { name: "comment", size: 280, wrap: true },
    {
      id: "comparisonComment",
      labelKey: "plugins.marketing.addressCampaignValue.comparisonComment",
      accessor: (row) => row.comparisonComment ?? null,
      size: 240,
      wrap: true,
      sortable: false,
      revealedByFilter: "comparisonCampaignId",
      cellClassName: COMPARISON_CELL_CLASS,
    },
  ],
  // The React list's row colours: an address that can't be mailed to reads red, a favorite of the user blue.
  legend: [
    { className: "row-red", labelKey: "address.filter.invalidAddresses" },
    { className: "row-blue", labelKey: "address.filter.myFavorites" },
  ],
  rowClassName: (row) => {
    if (row.isAddressValid === false) return "row-red";
    if (row.isFavoriteCard) return "row-blue";
    return undefined;
  },
  // An address without a value in the campaign has no stored value to open: its click opens the form for a new
  // one, preset from the address and the campaign (`newEntryParams` forwards the two to `newEntry`). Every other
  // row falls through to the normal edit page.
  onRowClick: (row) => {
    if (!isSyntheticRow(row)) return undefined;
    const campaignId = row.addressCampaign?.id;
    if (row.addressId == null || campaignId == null) return undefined;
    const params = new URLSearchParams({
      campaignId: String(campaignId),
      addressId: String(row.addressId),
    });
    return `${ADDRESS_CAMPAIGN_VALUE_ROUTE}/new?${params.toString()}`;
  },
  statistics: ({ statistics, isFetching }) => (
    <AddressCampaignValueCampaignLine
      statistics={statistics as AddressCampaignValueStatistics | undefined}
      isFetching={isFetching}
    />
  ),
  listActions: AddressCampaignValueListActions,
  // Served under `addressCampaignValueSelected` (AddressCampaignValueMultiSelectedPageRest): the value of the
  // selected campaign and the comment, for the selected rows — addresses without a value get one.
  massUpdate: {
    endpoint: "addressCampaignValueSelected",
    route: `${ADDRESS_CAMPAIGN_VALUE_ROUTE}/mass-update`,
    statisticsLine: ({ statistics }) => (
      <AddressCampaignValueCampaignLine
        statistics={statistics as AddressCampaignValueStatistics | undefined}
      />
    ),
  },
  edit: {
    schema: addressCampaignValueSchema,
    fieldNames: ADDRESS_CAMPAIGN_VALUE_FIELDS,
    defaultValues: emptyAddressCampaignValueValues,
    toFormValues,
    // Whose value it is.
    title: (entry) =>
      [entry.firstName, entry.name].filter(Boolean).join(" ") ||
      (entry.organization ?? ""),
    newTitleKey: "plugins.marketing.addressCampaignValue.title.add",
    savedMessageKey: "message.successfullChanged",
    newEntryParams: ["campaignId", "addressId"],
    editIntro: AddressCampaignValueEditIntro,
    sections: [
      {
        id: "value",
        titleKey: "plugins.marketing.addressCampaignValue",
        fields: [
          { custom: AddressCampaignValueSelectField, span: 3 },
          { name: "comment", span: 3, rows: 4 },
        ],
      },
    ],
  },
});
