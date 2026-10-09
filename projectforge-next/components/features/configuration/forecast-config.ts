/**
 * The JSON value of the configuration parameter `fibu.forecast`, mirroring `ForecastConfig`
 * (projectforge-business). Further forecast settings become further fields.
 */
export const FORECAST_PARAM = "fibu.forecast";

export interface ForecastConfig {
  /** Rich text (HTML) shown in the tooltip of the planning date of the forecast statistics. */
  planningDateHint: string | null;
}

/** The backend's defaults (`ForecastConfig`), for a parameter not set yet. */
export const FORECAST_DEFAULTS: ForecastConfig = {
  planningDateHint: null,
};

/**
 * The settings of the stored value, completed by the defaults. `invalid` if the value isn't a JSON object;
 * the defaults are taken then, as the backend does.
 */
export function parseForecastConfig(json: string | null | undefined): {
  config: ForecastConfig;
  invalid: boolean;
} {
  if (!json?.trim()) {
    return { config: FORECAST_DEFAULTS, invalid: false };
  }
  try {
    const value: unknown = JSON.parse(json);
    if (typeof value !== "object" || value === null || Array.isArray(value)) {
      return { config: FORECAST_DEFAULTS, invalid: true };
    }
    return { config: { ...FORECAST_DEFAULTS, ...value }, invalid: false };
  } catch {
    return { config: FORECAST_DEFAULTS, invalid: true };
  }
}
