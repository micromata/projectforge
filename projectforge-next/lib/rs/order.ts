/**
 * The two calls of the order book that are neither a list, a read nor a write of the entity: the live
 * sums of an unsaved form, and the forecast analysis.
 *
 * They are here rather than behind `postEntityAction` because they don't speak the `ResponseAction`
 * protocol: `recalculate` answers a plain sums object, and the forecast analysis answers an HTML
 * fragment. Both are GET/POST endpoints of `OrderEntityRest`.
 */

import { rawRequest, request, RsError } from "./client";
import { downloadPost, responseBlob, saveBlob } from "./download";
import type { MagicFilter, PostData } from "./types";

/** Sums of one position, matched by its number — a new position has no id yet. */
export interface OrderPositionSums {
  number?: number | null;
  netSum?: number | null;
  invoicedSum?: number | null;
  notYetInvoicedSum?: number | null;
  /**
   * Whether this position is due to be invoiced. Not the same as `notYetInvoicedSum > 0`, which holds for
   * every commissioned position that isn't fully invoiced yet: this one is true only once the position or
   * its order is closed, or a payment schedule entry of the position has been reached
   * (`OrderPositionInfo.recalculateAll`) — which is what Wicket's edit form highlights a position by.
   */
  toBeInvoiced?: boolean | null;
  /**
   * The probability the forecast applies to this position, as a factor between 0 and 1: it follows from the
   * status of the order *and* of the position, so the order's `probabilityOfOccurrence` field is only what
   * it falls back to (`ForecastUtils.getProbabilityOfAccurence`).
   */
  probabilityOfOccurrence?: number | null;
}

/** What `OrderEntityRest.recalculate` answers (`OrderSums` there). */
export interface OrderSums {
  netSum?: number | null;
  commissionedNetSum?: number | null;
  akquiseSum?: number | null;
  invoicedSum?: number | null;
  notYetInvoicedSum?: number | null;
  toBeInvoicedSum?: number | null;
  personDays?: number | null;
  /**
   * The probability of occurrence the forecast effectively works with, as a factor between 0 and 1:
   * weighted over the positions' net sums, because the probability itself is defined per position
   * (`ForecastUtils.getWeightedProbabilityOfAccurence`). Absent for an order without net sums.
   */
  weightedProbabilityOfOccurrence?: number | null;
  vollstaendigFakturiert: boolean;
  /** Something is due until the end of the current month (`OrderInfo.isToBeInvoicedBy`). */
  toBeInvoiced: boolean;
  /**
   * ISO date of the earliest reached payment schedule not yet invoiced — possibly in a following month.
   * Absent if nothing is to be invoiced.
   */
  nextInvoiceDate?: string | null;
  /** Something is to be invoiced right now, e.g. a finished position (`OrderInfo.toBeInvoicedImmediately`). */
  toBeInvoicedImmediately: boolean;
  /**
   * The period of performance over *all* positions, as ISO dates: the earliest begin and the latest end
   * any position effectively has. Computed by the backend, because which of the two dates a position
   * follows is its `periodOfPerformanceType`'s answer (`ForecastUtils.getStartLeistungszeitraum`), and
   * the order's own two dates are only what a position of type SEEABOVE refers to.
   */
  periodOfPerformanceBegin?: string | null;
  periodOfPerformanceEnd?: string | null;
  positions?: OrderPositionSums[] | null;
}

/**
 * Recalculates every sum of an order from the **unsaved** form state.
 *
 * Needed rather than convenient: the sums are computed by `OrderInfo.calculateAll` over the positions,
 * and `AuftragsCache` only knows saved orders — asking it about a form in progress answers 0,00 €. The
 * backend builds a transient `AuftragDO` from the posted DTO and computes on that; invoiced sums come
 * from the invoice cache for positions that already have an id, and count 0 for new ones (there cannot
 * be an invoice for a position that doesn't exist yet).
 *
 * @param data The form's values, i.e. the same `Auftrag` DTO a save would send.
 */
export function recalculateOrder(
  data: unknown,
  signal?: AbortSignal
): Promise<OrderSums> {
  const postData: PostData = { data } as PostData;
  return request<OrderSums>(
    "/rs/order/recalculate",
    { method: "POST", body: JSON.stringify(postData) },
    signal
  );
}

/**
 * The forecast analysis of a saved order, as the HTML fragment the backend renders
 * (`ForecastOrderAnalysis.htmlExport`) — a table of what is expected to be invoiced per month.
 *
 * Text, not JSON: the endpoint answers `String`. Rendering it is the caller's business, and the only
 * place in this app that inserts backend HTML (see the forecast tab).
 */
export async function fetchOrderForecastAnalysis(
  id: number,
  signal?: AbortSignal
): Promise<string> {
  const res = await rawRequest(
    `/rs/order/forecastAnalysis/${id}`,
    { method: "GET" },
    signal
  );
  if (!res.ok) {
    throw new RsError(
      res.status,
      `${res.status} ${res.statusText}: forecastAnalysis`
    );
  }
  return res.text();
}

/**
 * Downloads the forecast analysis as JSON — the numbers behind the table, for checking a forecast
 * against what the exports produce.
 *
 * Development only: the endpoint is gated by `SystemStatus.isDevelopmentMode()` and answers 404
 * otherwise. The caller hides the button outside development (the flag rides the system-status query,
 * see OrderForecastPanel); the 404 handling here stays as a fallback for the timing gap.
 */
export async function downloadOrderForecastJson(
  id: number,
  signal?: AbortSignal
): Promise<void> {
  const res = await rawRequest(
    `/rs/order/forecastAnalysisJson/${id}`,
    { method: "GET" },
    signal
  );
  if (!res.ok) {
    throw new RsError(
      res.status,
      `${res.status} ${res.statusText}: forecastAnalysisJson`
    );
  }
  // The endpoint sends no `Content-Disposition`, so the name is built here — from the order's id, which
  // is what identifies the export.
  saveBlob(await responseBlob(res), `forecast-order-${id}.json`);
}

/**
 * What the forecast export dialog asks for, mirroring `OrderEntityRest.ForecastExportSettings`.
 *
 * The backend remembers them per user, so the dialog is preset with the answer given last time — unlike
 * Wicket, which derives the start month from the period-of-performance filter without saying so.
 */
export interface ForecastExportSettings {
  /** First month of the forecast as `yyyy-MM-dd`; the backend takes the begin of its month. */
  startDate: string | null;
  /** The optimistic variant — unused budget booked as future revenue. */
  distributeUnusedBudget: boolean;
}

/** React Query key of the settings, shared by the dialog that reads them and the export that writes them. */
export const FORECAST_SETTINGS_QUERY_KEY = [
  "order",
  "forecastExportSettings",
] as const;

/** The stored settings, or the backend's defaults when the user hasn't exported yet. */
export function fetchForecastExportSettings(
  signal?: AbortSignal
): Promise<ForecastExportSettings> {
  return request<ForecastExportSettings>(
    "/rs/order/forecastExportSettings",
    { method: "GET" },
    signal
  );
}

/**
 * The filtered order list as the three-sheet Excel file of `OrderExport` — what Wicket's "Excel export"
 * produces.
 */
export function downloadOrderExcel(
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<void> {
  return downloadPost("/rs/order/exportAsExcel", filter, signal);
}

/**
 * The forecast of the filtered orders as xlsx. The settings travel with the filter and are persisted by
 * the backend on the way, so a following [fetchForecastExportSettings] answers them.
 */
export function downloadOrderForecast(
  filter: MagicFilter,
  settings: ForecastExportSettings,
  signal?: AbortSignal
): Promise<void> {
  return downloadPost("/rs/order/exportForecast", { filter, settings }, signal);
}

/**
 * The parameters of the forecast charts tab, mirroring `OrderEntityRest.ForecastChartSettings`; remembered
 * per user by the backend on every chart request.
 */
export interface ForecastChartSettings {
  /** First month of the forecast as `yyyy-MM-dd`; the backend takes the begin of its month. */
  startDate: string | null;
  /** Day of the order book snapshot used as plan (the closest one), or null for no plan. */
  planningDate: string | null;
  /**
   * Optimistic (true, the unused budget distributed) or conservative (false, run rate; lost budget warnings)
   * forecast, see `ForecastOrderPosInfo.distributeUnusedBudget`.
   */
  distributeUnusedBudget: boolean;
}

/**
 * The months of performance a time & materials position needs before its run rate counts in the
 * conservative forecast, mirroring `ForecastOrderPosInfo.RUN_RATE_MIN_ELAPSED_MONTHS` (argument of the
 * variant's explanation).
 */
export const FORECAST_RUN_RATE_MIN_ELAPSED_MONTHS = 3;

/** The position statuses the forecast sums up, in the order of the Excel template (rows 2-6). */
export const FORECAST_CHART_STATUSES = [
  "BEAUFTRAGT",
  "GELEGT",
  "LOI",
  "IN_ERSTELLUNG",
  "POTENZIAL",
] as const;

export type ForecastChartStatus = (typeof FORECAST_CHART_STATUSES)[number];

/**
 * The monthly totals of the forecast charts, see `ForecastChartData`: 12 entries per list, one per month
 * of `months`.
 */
export interface ForecastChartData {
  /** The 12 months as `yyyy-MM`. */
  months: string[];
  /** Remaining forecast per month by position status. */
  forecastByStatus: Partial<Record<ForecastChartStatus, number[]>>;
  ist: number[];
  prevYear: number[];
  prevPrevYear: number[];
  /** Per month max(IST, forecast) — the base of the cumulated forecast. */
  total: number[];
  /** Null if no planning date was given. */
  plan: number[] | null;
  /** The snapshot date actually used for the plan (`yyyy-MM-dd`). */
  planningDate: string | null;
}

/** The stored parameters of the charts tab, or the backend's defaults (begin of the year, no plan). */
export function fetchForecastChartSettings(
  signal?: AbortSignal
): Promise<ForecastChartSettings> {
  return request<ForecastChartSettings>(
    "/rs/order/forecastChart/settings",
    { method: "GET" },
    signal
  );
}

/**
 * The forecast chart totals of the filtered orders. `months` is empty if neither order positions nor
 * invoices were found.
 */
export function fetchForecastChart(
  filter: MagicFilter,
  settings: ForecastChartSettings,
  signal?: AbortSignal
): Promise<ForecastChartData> {
  return request<ForecastChartData>(
    "/rs/order/forecastChart",
    { method: "POST", body: JSON.stringify({ filter, ...settings }) },
    signal
  );
}

/** One project of the forecast's project overview (`ForecastProjectRow`). */
export interface ForecastProjectRow {
  /** -1 (`PROJECT_ID_NONE`) for the invoices and positions without any project. */
  projectId: number;
  customer: string | null;
  project: string | null;
  /** Remaining forecast plus the invoices (IST) of the 12 months. */
  forecast: number;
  /** Null if no planning date was given. */
  plan: number | null;
  prevYear: number;
  prevPrevYear: number;
  /** The sum of the differences of the project's positions. */
  difference: number;
  /** The lost budget warnings of the project's positions. */
  warnings: ForecastWarning[];
}

/** The lost budget warning of an order position (`ForecastWarning`). */
export interface ForecastWarning {
  /** Order and position number, e.g. `7076.1`. */
  position: string;
  text: string;
}

/** One order position of the forecast, a row of the Excel's Forecast_Data (`ForecastPositionRow`). */
export interface ForecastPositionRow {
  /** Null for the pseudo rows (invoices without order resp. without project). */
  orderId: number | null;
  orderNumber: number | null;
  positionNumber: number | null;
  projectId: number | null;
  customer: string | null;
  project: string | null;
  title: string | null;
  /** Only given if it differs from the order's title. */
  positionTitle: string | null;
  art: string | null;
  paymentType: string | null;
  /** Translated, as in the Excel. */
  orderStatus: string;
  positionStatus: string;
  personDays: number | null;
  netSum: number;
  probability: number;
  weightedNetSum: number;
  invoicedSum: number;
  toBeInvoicedSum: number;
  periodOfPerformanceBegin: string | null;
  periodOfPerformanceEnd: string | null;
  forecastType: string;
  /** The remaining forecast of the 12 months; null for no value. */
  months: (number | null)[];
  /** The remaining forecast after the 12 months. */
  remaining: number;
  difference: number;
  /** The lost budget warning of the conservative forecast, or null. */
  warning: string | null;
  /** The indexes 0..11 of the months with a lost budget warning, marked red as in the Excel. */
  warningMonths: number[];
  pseudo: boolean;
}

export type ForecastInvoiceKind = "IST" | "PREV_YEAR" | "PREV_PREV_YEAR";

/** One invoice position of the forecast's invoice sheets (`ForecastInvoiceRow`). */
export interface ForecastInvoiceRow {
  invoiceId: number | null;
  invoiceNumber: number | null;
  positionNumber: number | null;
  date: string | null;
  projectId: number;
  customer: string | null;
  project: string | null;
  subject: string | null;
  positionText: string | null;
  orderId: number | null;
  /** The order position as `<order number>.<position number>`. */
  order: string | null;
  netSum: number;
  /** Index 0..11 of the month within the 12 months of `kind`. */
  monthIndex: number;
  kind: ForecastInvoiceKind;
}

/** The rows behind the forecast charts (`ForecastTables`), of the same request as {@link fetchForecastChart}. */
export interface ForecastTables {
  /** The 12 months as `yyyy-MM`. */
  months: string[];
  projects: ForecastProjectRow[];
  positions: ForecastPositionRow[];
  invoices: ForecastInvoiceRow[];
}

/**
 * The rows behind {@link fetchForecastChart} of the same filter and dates. The backend caches the
 * calculation, so asking right after the charts doesn't run the forecast again. Stores neither filter nor
 * dates.
 */
export function fetchForecastTables(
  filter: MagicFilter,
  settings: ForecastChartSettings,
  signal?: AbortSignal
): Promise<ForecastTables> {
  return request<ForecastTables>(
    "/rs/order/forecastChart/tables",
    { method: "POST", body: JSON.stringify({ filter, ...settings }) },
    signal
  );
}

/**
 * The parameter of the contribution margin tab, mirroring `OrderEntityRest.ContributionMarginSettings`;
 * remembered per user by the backend on every request.
 */
export interface ContributionMarginSettings {
  /** First month of the period as `yyyy-MM-dd`; the backend takes the begin of its month. */
  startDate: string | null;
}

/**
 * Revenue, costs (positive) and contribution margin of the period, and the contribution margin of the same
 * period of the two previous years (`ContributionMarginSums`). `percentage` is null without positive revenue.
 */
export interface ContributionMarginSums {
  revenue: number;
  costs: number;
  profit: number;
  percentage: number | null;
  prevYearProfit: number;
  prevPrevYearProfit: number;
  /** Revenue of the same period one year earlier, and its DB % (null without positive revenue). */
  prevYearRevenue: number;
  prevYearPercentage: number | null;
  /** Revenue of the same period two years earlier, and its DB % (null without positive revenue). */
  prevPrevYearRevenue: number;
  prevPrevYearPercentage: number | null;
}

export interface ContributionMarginProject extends ContributionMarginSums {
  projectId: number;
  kost: string | null;
  customer: string | null;
  project: string | null;
}

/**
 * The contribution margin of the projects of the filtered orders, see `ContributionMarginData`: 12 entries
 * per monthly list, one per month of `months`.
 */
export interface ContributionMarginData {
  /** The 12 months as `yyyy-MM`. */
  months: string[];
  revenue: number[];
  /** Positive amounts. */
  costs: number[];
  profit: number[];
  /**
   * Contribution margin in % of the revenue per month, cumulated from the first month on (so it settles in the
   * course of the period); 0 for a loss, null as long as there is no positive revenue.
   */
  percentage: (number | null)[];
  /** Whether the month lies after the last imported accounting records, so its values are preliminary. */
  preliminary: boolean[];
  /**
   * The last month with values (`yyyy-MM`): the previous month at the latest, as the current one isn't
   * complete yet. Null if the period begins in the current month or later; missing from an older backend.
   */
  lastMonth?: string | null;
  /**
   * The last day (`yyyy-MM-dd`) the sums of the period cover, and those of the same months one and two years
   * earlier (their whole 12 months, the end of the previous month at the latest). Null if the respective
   * period has no values yet; missing from an older backend.
   */
  valuesEnd?: string | null;
  prevYearValuesEnd?: string | null;
  prevPrevYearValuesEnd?: string | null;
  /** Monthly contribution margin of the same months one and two years earlier. */
  prevYear: number[];
  prevPrevYear: number[];
  /** Cumulated DB % of the same months one year earlier, like `percentage`. */
  prevYearPercentage: (number | null)[];
  projects: ContributionMarginProject[];
  total: ContributionMarginSums;
  /** Last day of the last imported month (`yyyy-MM-dd`), or null if no accounting records exist. */
  bookingImportEnd: string | null;
  hourlyRate: number | null;
  /** Preliminary months exist, but no hourly rate is configured: their costs lack the time sheets. */
  hourlyRateMissing: boolean;
  /** The target contribution margin in %: green from here on, yellow below (see ContributionMarginLimits). */
  targetPercentage: number;
  /** The contribution margin in % below which it is red. */
  redThreshold: number;
  ordersWithoutProject: number;
}

/** The stored start date of the contribution margin tab, or the backend's default (begin of the year). */
export function fetchContributionMarginSettings(
  signal?: AbortSignal
): Promise<ContributionMarginSettings> {
  return request<ContributionMarginSettings>(
    "/rs/order/contributionMargin/settings",
    { method: "GET" },
    signal
  );
}

/** The contribution margin of the projects of the filtered orders (those the user may see). */
export function fetchContributionMargin(
  filter: MagicFilter,
  settings: ContributionMarginSettings,
  signal?: AbortSignal
): Promise<ContributionMarginData> {
  return request<ContributionMarginData>(
    "/rs/order/contributionMargin",
    { method: "POST", body: JSON.stringify({ filter, ...settings }) },
    signal
  );
}

/** The sums of a project in a month of the period (`ContributionMarginMonthRow`). */
export interface ContributionMarginMonthRow {
  /** `yyyy-MM`. */
  month: string;
  projectId: number;
  kost: string | null;
  customer: string | null;
  project: string | null;
  revenue: number;
  /** Positive amounts. */
  costs: number;
  profit: number;
  percentage: number | null;
  /** The month contains preliminary values (unbooked invoices, time sheets). */
  preliminary: boolean;
}

/** One invoice position of a project in the period (`ContributionMarginInvoiceRow`). */
export interface ContributionMarginInvoiceRow {
  invoiceId: number | null;
  date: string | null;
  number: number | null;
  positionNumber: number | null;
  projectId: number | null;
  kost: string | null;
  customer: string | null;
  project: string | null;
  subject: string | null;
  netSum: number;
  status: string | null;
  orderId: number | null;
  /** The order position as `<order number>.<position number>`. */
  order: string | null;
  /** Date of the accounting record, null if not booked (yet). */
  bookedDate: string | null;
  /** Counts as preliminary revenue (not booked yet). */
  preliminary: boolean;
}

/** The time sheet costs (hours × hourly rate) of a kost2 in a month, preliminary by definition. */
export interface ContributionMarginTimesheetRow {
  /** `yyyy-MM`. */
  month: string;
  projectId: number;
  kost2: string | null;
  customer: string | null;
  project: string | null;
  hours: number | null;
  /** Positive amount. */
  costs: number;
}

/** The rows behind the contribution margin of the period (`ContributionMarginDetails`). */
export interface ContributionMarginDetails {
  months: ContributionMarginMonthRow[];
  invoices: ContributionMarginInvoiceRow[];
  timesheets: ContributionMarginTimesheetRow[];
}

/**
 * The rows behind {@link fetchContributionMargin} of the same filter and start date, from the backend's
 * cache of that calculation. Stores neither filter nor start date.
 */
export function fetchContributionMarginDetails(
  filter: MagicFilter,
  settings: ContributionMarginSettings,
  signal?: AbortSignal
): Promise<ContributionMarginDetails> {
  return request<ContributionMarginDetails>(
    "/rs/order/contributionMargin/details",
    { method: "POST", body: JSON.stringify({ filter, ...settings }) },
    signal
  );
}

/** React Query key of whether the logged-in user may refresh the order caches. */
export const REFRESH_CACHE_ACCESS_QUERY_KEY = [
  "order",
  "refreshCacheAccess",
] as const;

/** Whether the logged-in user may use {@link refreshOrderCache}: the finance staff only. */
export function fetchRefreshCacheAccess(
  signal?: AbortSignal
): Promise<{ access: boolean }> {
  return request<{ access: boolean }>(
    "/rs/order/refreshCacheAccess",
    { method: "GET" },
    signal
  );
}

/**
 * Rebuilds the invoice and order caches, so the invoiced sums and invoice links of the orders reflect the
 * current invoices at once. Answers the translated confirmation.
 */
export function refreshOrderCache(): Promise<{ message: string }> {
  return request<{ message: string }>("/rs/order/refreshCache", {
    method: "POST",
  });
}
