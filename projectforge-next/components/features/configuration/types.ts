// Mirrors org.projectforge.rest.dto.Configuration (projectforge-rest). A configuration parameter has a
// polymorphic value held per `configurationType` in one of four typed slots; only the slot matching the
// type is populated (the others are absent, JsonInclude.Include.NON_NULL).

/**
 * The value types of a configuration parameter (org.projectforge.framework.configuration.ConfigurationType).
 * Serialized as the enum name (`@Enumerated(STRING)`), so these are exactly the strings on the wire. Only
 * the ones the current parameter set uses have an editor (see ConfigurationValueField); INTEGER, TASK and
 * CALENDAR are carried for completeness but not editable through this page.
 */
export const CONFIGURATION_TYPES = [
  "STRING",
  "TEXT",
  "LONG",
  "INTEGER",
  "FLOAT",
  "BOOLEAN",
  "PERCENT",
  "TASK",
  "TIME_ZONE",
  "CALENDAR",
  "JSON",
] as const;

export type ConfigurationType = (typeof CONFIGURATION_TYPES)[number];

/**
 * A configuration parameter as the DTO carries it. Every optional property is `?`, not just `| null`:
 * Spring's mapper uses `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty value slot is
 * absent from the JSON rather than null. `toFormValues` normalises that away.
 */
export interface ConfigurationDetail {
  /** Never null in practice — the parameter set is fixed and every row exists (checkAndUpdateDatabaseEntries). */
  id: number | null;
  /** The parameter key (the ConfigurationParam name); read-only, shown but never edited. */
  parameter?: string | null;
  /** The value type, which selects the editor and the value slot. */
  configurationType?: ConfigurationType | null;
  /** i18n key of the parameter label, `administration.configuration.param.<parameter>`. */
  i18nKey?: string | null;
  /** i18n key of the parameter description, `<i18nKey>.description`. */
  descriptionI18nKey?: string | null;
  /** Translated parameter label in the user's locale — the edit page heading (see Configuration.label). */
  label?: string | null;
  /** Value slot for STRING, TEXT, JSON and TIME_ZONE (a time-zone id). */
  stringValue?: string | null;
  /** Value slot for LONG. */
  longValue?: number | null;
  /** Value slot for FLOAT and PERCENT (a factor, e.g. 0.19 shown as 19 %). */
  floatValue?: number | null;
  /** Value slot for BOOLEAN. */
  booleanValue?: boolean | null;
  /**
   * Route of the page the parameter is maintained on, if it has one of its own (the customer groups):
   * a row opens it directly, and it is shown read-only here and linked there (ConfigurationParam.getEditPage).
   * Only sent to the parameter's editors; anyone else could not open it.
   */
  editPage?: string | null;
  /**
   * A TEXT parameter whose value is rich text (the HTML of RichTextEditor), edited with that editor
   * instead of a textarea (ConfigurationParam.isRichText).
   */
  richText?: boolean;
  /** False where the user may only look (EntityAccessSupport, see lib/rs/entity-access.ts). */
  writeAccess?: boolean;
  /** `boolean` (not `| null`): NON_NULL omits it for a row that isn't deleted, so it matches ListRow. */
  deleted?: boolean;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface ConfigurationRow extends ConfigurationDetail {
  id: number;
}
