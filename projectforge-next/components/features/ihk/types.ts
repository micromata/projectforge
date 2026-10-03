/** Contract of `org.projectforge.plugins.ihk.IHKRest` (/rs/ihk). */

/** The apprentice's settings, read from the JSON in the comment of the user's own address. */
export interface IhkSettings {
  /** ISO date. */
  ausbildungsbeginn: string;
  /** -1: calculated from the training start. */
  ausbildungsjahr: number;
  teamname?: string;
}

export type IhkSettingsErrorReason = "notFound" | "empty" | "parsing";

export interface IhkSettingsError {
  reason: IhkSettingsErrorReason;
  /** The JSON parser's message, for `parsing` only. */
  detail?: string;
}

export interface IhkInit {
  /** The user's names, which the address must match exactly. */
  firstname?: string;
  lastname?: string;
  /** Either `settings` or `settingsError` is given. */
  settings?: IhkSettings;
  settingsError?: IhkSettingsError;
  /** Edit page of the user's address, or the new-address page if none was found (a menu url). */
  addressUrl: string;
  logViewerUrl?: string;
}

/** A time sheet of the week without description, which would be an empty row of the report. */
export interface IhkMissingDescription {
  id: number;
  /** The cost unit's description or name. */
  label: string;
  /** ISO instants. */
  startTime?: string;
  stopTime?: string;
}
