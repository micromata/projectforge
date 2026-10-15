// Mirrors org.projectforge.plugins.marketing.dto.AddressCampaign (marketing plugin). Keep field names in
// sync with the Spring DTO.

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface AddressCampaignDetail {
  /** null for a campaign that has not been saved yet (Spring assigns the id). */
  id: number | null;
  title?: string | null;
  /** The semicolon separated values an address can be given in this campaign, e.g. "Yes; No". */
  values?: string | null;
  comment?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface AddressCampaignListRow extends AddressCampaignDetail {
  id: number;
}
