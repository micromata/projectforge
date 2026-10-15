/** Contract of `org.projectforge.plugins.ihk.IHKRest` (/rs/ihk). */

/** The apprentice's settings, a user pref edited on the page (`IHKSettings`). */
export interface IhkSettings {
  /** ISO date; required for a report. */
  ausbildungsbeginn?: string;
  /** {@link IHK_AUSBILDUNGSJAHR_AUTO}: calculated from the training start. */
  ausbildungsjahr: number;
  teamname?: string;
}

/** The training year calculated from the training start (`IHKSettings.AUSBILDUNGSJAHR_AUTO`). */
export const IHK_AUSBILDUNGSJAHR_AUTO = -1;

/** The training years that may be chosen explicitly (`IHKSettings.AUSBILDUNGSJAHRE`). */
export const IHK_AUSBILDUNGSJAHRE = [1, 2, 3, 4] as const;

export interface IhkInit {
  /** Missing while the user has not set them up. */
  settings?: IhkSettings;
  /** The settings were just taken over from the comment of the user's address (the former setup). */
  migratedFromAddress: boolean;
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
