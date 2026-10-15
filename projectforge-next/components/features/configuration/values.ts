import type { ConfigurationValues } from "./schema";
import type { ConfigurationDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` and the value slot
 * not matching the parameter's type stays a clean `null`.
 */
export function toFormValues(detail: ConfigurationDetail): ConfigurationValues {
  return {
    id: detail.id ?? null,
    parameter: detail.parameter ?? null,
    configurationType: detail.configurationType ?? null,
    i18nKey: detail.i18nKey ?? null,
    descriptionI18nKey: detail.descriptionI18nKey ?? null,
    stringValue: detail.stringValue ?? null,
    longValue: detail.longValue ?? null,
    floatValue: detail.floatValue ?? null,
    booleanValue: detail.booleanValue ?? null,
  };
}

/**
 * Blank form for a parameter that doesn't exist yet. Only to satisfy the generic edit page's add
 * contract — the parameter set is fixed and `ConfigurationDao` refuses inserts, so this is unreachable.
 */
export function emptyConfigurationValues(): ConfigurationValues {
  return {
    id: null,
    parameter: null,
    configurationType: null,
    i18nKey: null,
    descriptionI18nKey: null,
    stringValue: null,
    longValue: null,
    floatValue: null,
    booleanValue: null,
  };
}
