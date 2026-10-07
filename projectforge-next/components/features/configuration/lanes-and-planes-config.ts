/**
 * The JSON value of the configuration parameter `lanesAndPlanes`, mirroring `LanesAndPlanesSettings`
 * (projectforge-business). The backend validates it on saving and reports the errors on `stringValue`.
 */
export const LANES_AND_PLANES_PARAM = "lanesAndPlanes";

/** A user of Lanes & Planes who isn't an employee, sent with name and email only. */
export interface LanesAndPlanesAdditionalUser {
  email: string;
  firstName: string;
  lastName: string;
}

export const EMPTY_ADDITIONAL_USER: LanesAndPlanesAdditionalUser = {
  email: "",
  firstName: "",
  lastName: "",
};

export interface LanesAndPlanesConfig {
  /** Provided by Lanes & Planes; an empty row is posted as null and ignored by the backend. */
  accountingInvoiceProfileIds: (number | null)[];
  /** Formatted numbers of the Kost1 sent to all users as cost centers, e.g. `1.005.01.00`. */
  generalKost1: string[];
  /** Formatted numbers of the Kost2 sent to all users as cost units, e.g. `5.999.10.09`. */
  generalKost2: string[];
  /** Glob patterns (`*`, `?`) of the Kost2 a user may book, e.g. `5.*.02`. Empty: all. */
  kost2Patterns: string[];
  /** Users who aren't employees (e.g. an external accountant), so the push doesn't deactivate them. */
  additionalUsers: LanesAndPlanesAdditionalUser[];
}

/** The backend's defaults (`LanesAndPlanesSettings`), for a parameter not set yet. */
export const LANES_AND_PLANES_DEFAULTS: LanesAndPlanesConfig = {
  accountingInvoiceProfileIds: [],
  generalKost1: [],
  generalKost2: [],
  kost2Patterns: [],
  additionalUsers: [],
};

function arrayOf<T>(value: unknown): T[] {
  return Array.isArray(value) ? (value as T[]) : [];
}

/**
 * The settings of the stored value, completed by the defaults. `invalid` if the value isn't a JSON object;
 * the defaults are taken then, as the backend does.
 */
export function parseLanesAndPlanesConfig(json: string | null | undefined): {
  config: LanesAndPlanesConfig;
  invalid: boolean;
} {
  if (!json?.trim()) {
    return { config: LANES_AND_PLANES_DEFAULTS, invalid: false };
  }
  try {
    const value: unknown = JSON.parse(json);
    if (typeof value !== "object" || value === null || Array.isArray(value)) {
      return { config: LANES_AND_PLANES_DEFAULTS, invalid: true };
    }
    const config = {
      ...LANES_AND_PLANES_DEFAULTS,
      ...value,
    } as LanesAndPlanesConfig;
    return {
      config: {
        accountingInvoiceProfileIds: arrayOf(
          config.accountingInvoiceProfileIds
        ),
        generalKost1: arrayOf(config.generalKost1),
        generalKost2: arrayOf(config.generalKost2),
        kost2Patterns: arrayOf(config.kost2Patterns),
        additionalUsers: arrayOf<Partial<LanesAndPlanesAdditionalUser>>(
          config.additionalUsers
        ).map((user) => ({ ...EMPTY_ADDITIONAL_USER, ...user })),
      },
      invalid: false,
    };
  } catch {
    return { config: LANES_AND_PLANES_DEFAULTS, invalid: true };
  }
}

/**
 * The entries of a list setting typed or pasted as text: separated by commas, semicolons or line breaks
 * (a CSV line or a column copied from a spreadsheet), trimmed, empty ones dropped.
 */
export function splitEntries(text: string): string[] {
  return text
    .split(/[,;\r\n]+/)
    .map((entry) => entry.trim())
    .filter((entry) => entry.length > 0);
}

/** The formatted number leading the display name of a Kost1 or Kost2 (`1.234.56.78: …`), null if there is none. */
export function kostNumberOf(displayName: string): string | null {
  return /^\d+(?:\.\d+)*/.exec(displayName.trim())?.[0] ?? null;
}
