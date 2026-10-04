import type { MonthlyReportQuery } from "./types";

function initialNumber(
  params: URLSearchParams,
  key: string
): number | undefined {
  const value = Number(params.get(key));
  return value > 0 ? value : undefined;
}

const URL_KEYS = ["userId", "year", "month"] as const;

/**
 * Writes the filter back into the deep-link parameters, so a reload or a copied link shows the same report.
 * `replaceState`, not `pushState`: stepping through the months should not fill the browser history. Unset
 * values are dropped — the server then picks its default (the logged-in user, the current month).
 */
export function writeQueryToUrl(query: MonthlyReportQuery): void {
  const params = new URLSearchParams(window.location.search);
  for (const key of URL_KEYS) {
    const value = query[key];
    if (value) params.set(key, String(value));
    else params.delete(key);
  }
  const search = params.toString();
  window.history.replaceState(
    null,
    "",
    search ? `?${search}` : window.location.pathname
  );
}

/** The filter the deep-link `?userId=&year=&month=` names; a missing or invalid value stays unset. */
export function readQueryFromUrl(params: URLSearchParams): MonthlyReportQuery {
  return {
    userId: initialNumber(params, "userId"),
    year: initialNumber(params, "year"),
    month: initialNumber(params, "month"),
  };
}
