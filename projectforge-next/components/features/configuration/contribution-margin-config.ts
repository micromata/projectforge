/**
 * The JSON value of the configuration parameter `fibu.contributionMargin`, mirroring
 * `ContributionMarginConfig` (projectforge-business). The backend validates it on saving and reports the
 * errors on `stringValue`.
 */
export const CONTRIBUTION_MARGIN_PARAM = "fibu.contributionMargin";

export interface Kost2Assignment {
  /** The first three parts of the kost2 numbers, e.g. `6.000.10`. */
  kost2: string | null;
  /** The kost of the project, e.g. `5.999.10`. */
  project: string | null;
}

export interface ContributionMarginConfig {
  revenueAccounts: string | null;
  hourlyRate: number | null;
  targetPercentage: number;
  redThreshold: number;
  kost2Assignments: Kost2Assignment[];
  /** Free text about the settings, e.g. how the calculated rate is derived; not evaluated. */
  remark: string | null;
}

/** The backend's defaults (`ContributionMarginConfig`), for a parameter not set yet. */
export const CONTRIBUTION_MARGIN_DEFAULTS: ContributionMarginConfig = {
  revenueAccounts: "4000-4799",
  hourlyRate: null,
  targetPercentage: 65,
  redThreshold: 50,
  kost2Assignments: [],
  remark: null,
};

/**
 * The settings of the stored value, completed by the defaults. `invalid` if the value isn't a JSON object;
 * the defaults are taken then, as the backend does.
 */
export function parseContributionMarginConfig(
  json: string | null | undefined
): {
  config: ContributionMarginConfig;
  invalid: boolean;
} {
  if (!json?.trim()) {
    return { config: CONTRIBUTION_MARGIN_DEFAULTS, invalid: false };
  }
  try {
    const value: unknown = JSON.parse(json);
    if (typeof value !== "object" || value === null || Array.isArray(value)) {
      return { config: CONTRIBUTION_MARGIN_DEFAULTS, invalid: true };
    }
    const config = { ...CONTRIBUTION_MARGIN_DEFAULTS, ...value };
    return {
      config: {
        ...config,
        kost2Assignments: Array.isArray(config.kost2Assignments)
          ? config.kost2Assignments
          : [],
      },
      invalid: false,
    };
  } catch {
    return { config: CONTRIBUTION_MARGIN_DEFAULTS, invalid: true };
  }
}
