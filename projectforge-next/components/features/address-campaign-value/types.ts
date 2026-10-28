// Mirrors org.projectforge.plugins.marketing.dto.AddressCampaignValue (marketing plugin). Keep field names in
// sync with the Spring DTO.

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface AddressCampaignValueDetail {
  /**
   * null for a value that has not been saved yet. In the list, an address without a value in the
   * campaign comes with its negated address id as synthetic id (see isSyntheticRow).
   */
  id: number | null;
  /** The campaign, with its values (`valuesArray`) the value is one of. */
  addressCampaign?: {
    id?: number | null;
    title?: string | null;
    valuesArray?: string[] | null;
    /** An inactive campaign is closed: its values are read only (AddressCampaignValueRight). */
    active?: boolean | null;
  } | null;
  addressId?: number | null;
  name?: string | null;
  fullLastName?: string | null;
  firstName?: string | null;
  organization?: string | null;
  /** The business and the private e-mail address, joined. */
  email?: string | null;
  contactStatus?: string | null;
  contactStatusAsString?: string | null;
  addressStatus?: string | null;
  addressStatusAsString?: string | null;
  /** The mailing address, one line per row. */
  formattedAddress?: string | null;
  value?: string | null;
  comment?: string | null;
  isFavoriteCard?: boolean | null;
  isAddressValid?: boolean | null;
  lastUpdate?: string | null;
  lastUpdateOfAddress?: string | null;
  /** "3 days ago" of lastUpdate / lastUpdateOfAddress, translated by the backend. */
  timeAgo?: string | null;
  timeAgoOfAddress?: string | null;
  /**
   * The value and comment the address has in the comparison campaign the list shows beside this one (filter
   * `comparisonCampaignId`), and — for the form only — that campaign's title.
   */
  comparisonValue?: string | null;
  comparisonComment?: string | null;
  comparisonCampaignTitle?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface AddressCampaignValueListRow extends AddressCampaignValueDetail {
  id: number;
}

/** What `AddressCampaignValueEntityRest` sends as `ResultSet.statistics`: the selected campaign, if any. */
export interface AddressCampaignValueStatistics {
  campaignId?: number | null;
  campaignTitle?: string | null;
  /** false for an inactive campaign, whose values can't be changed any more. */
  campaignActive?: boolean | null;
  comparisonCampaignId?: number | null;
  comparisonCampaignTitle?: string | null;
}

/**
 * A row of an address without a value in the campaign yet: there is no stored value to open, so a click opens
 * the form for a new one (see ADDRESS_CAMPAIGN_VALUE_PAGE.onRowClick).
 */
export function isSyntheticRow(row: AddressCampaignValueDetail): boolean {
  return row.id != null && row.id < 0;
}
