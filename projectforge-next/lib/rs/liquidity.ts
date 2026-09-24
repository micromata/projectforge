/**
 * The calls of the liquidity plugin (`LiquidityEntityRest`) that are neither a list, a read nor a write of
 * the entity: the Excel export of the filtered list and the per-day liquidity forecast that drives the
 * forecast tab of `/next/liquidity`.
 */

import { request } from "./client";
import { downloadListExcel } from "./list-export";
import type { MagicFilter } from "./types";

/** REST category of the liquidity plugin — `LiquidityEntityRest` is mapped to "liquidity". */
const ENTITY = "liquidity";

/**
 * The filtered liquidity entries as the Excel file Wicket's "Excel export" produces — one row per entry.
 *
 * The generic list export of this category, so it goes through [downloadListExcel]. A 404 means the filter
 * matched nothing; the caller says so rather than reporting an error (see LiquidityListActions).
 */
export function downloadLiquidityExcel(
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<void> {
  return downloadListExcel(ENTITY, filter, signal);
}

/** The parameters of the forecast, as `LiquidityEntityRest.ForecastRequest` takes them. */
export interface LiquidityForecastRequest {
  /** The balance the running sums start from, in the system currency. */
  startAmount: number;
  /** The day the forecast starts, ISO `yyyy-MM-dd`. A future date is clamped to today by the backend. */
  baseDate: string | null;
  /** How many days to forecast, clamped to `LiquidityForecastSettings.MAX_FORECAST_DAYS` (600). */
  nextDays: number;
}

/** One day of the forecast — see `LiquidityEntityRest.ForecastDay`. */
export interface LiquidityForecastDay {
  date: string;
  /** Running balance by actual due date. */
  dueDateBalance: number;
  /** Running balance by expected date of payment. */
  expectedBalance: number;
  /** The day's expected credit (negative — money coming in). */
  creditExpected: number;
  /** The day's expected debit (positive — money going out). */
  debitExpected: number;
}

/** What `LiquidityEntityRest.getForecast` answers — the echoed parameters and one point per day. */
export interface LiquidityForecastResult {
  baseDate: string;
  startAmount: number;
  nextDays: number;
  points: LiquidityForecastDay[];
}

/**
 * The per-day liquidity forecast for the given parameters (`POST /rs/liquidity/forecast`). Read only; the
 * backend checks the select access of the category. Posting a forecast also persists its parameters as the
 * user's preference (see [fetchLiquidityForecastSettings]).
 */
export function fetchLiquidityForecast(
  params: LiquidityForecastRequest,
  signal?: AbortSignal
): Promise<LiquidityForecastResult> {
  return request<LiquidityForecastResult>(
    `/rs/${ENTITY}/forecast`,
    { method: "POST", body: JSON.stringify(params) },
    signal
  );
}

/**
 * The forecast parameters the user last used, so the tab can seed its controls on open — the successor of
 * the Wicket page's persisted `LiquidityForecastSettings`. Any field is null when the user has never run a
 * forecast (`GET /rs/liquidity/forecast/settings`).
 */
export interface LiquidityForecastSettings {
  startAmount: number | null;
  baseDate: string | null;
  nextDays: number | null;
}

/** The user's last-used forecast parameters (`GET /rs/liquidity/forecast/settings`). Read only. */
export function fetchLiquidityForecastSettings(
  signal?: AbortSignal
): Promise<LiquidityForecastSettings> {
  return request<LiquidityForecastSettings>(
    `/rs/${ENTITY}/forecast/settings`,
    { method: "GET" },
    signal
  );
}
