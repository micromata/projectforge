import { request } from "./client";

/**
 * The deployment's customer-specific i18n overrides for a locale — a flat map of dotted keys to texts,
 * or `{}` when this deployment ships no `CustomerI18nResources` bundle.
 *
 * Public endpoint (no login): the overlay is applied around the whole app, the login page included, and
 * i18n labels are not sensitive. The frontend overlays these on its static catalog (see
 * `i18n/config.ts:applyCustomerOverrides`), mirroring the highest-priority bundle the server-rendered
 * pages resolved through `I18nHelper` automatically.
 */
export function fetchCustomerI18nOverrides(
  locale: string,
  signal?: AbortSignal
): Promise<Record<string, string>> {
  return request<Record<string, string>>(
    `/rsPublic/i18nCustomerOverrides?locale=${encodeURIComponent(locale)}`,
    { method: "GET" },
    signal
  );
}
