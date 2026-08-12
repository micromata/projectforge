/**
 * The two calls of the order book that are neither a list, a read nor a write of the entity: the live
 * sums of an unsaved form, and the forecast analysis.
 *
 * They are here rather than behind `postEntityAction` because they don't speak the `ResponseAction`
 * protocol: `recalculate` answers a plain sums object, and the forecast analysis answers an HTML
 * fragment. Both are GET/POST endpoints of `AuftragPagesRest`.
 */

import { rawRequest, request, RsError } from "./client";
import { saveBlob } from "./download";
import type { PostData } from "./types";

/** Sums of one position, matched by its number — a new position has no id yet. */
export interface OrderPositionSums {
  number?: number | null;
  netSum?: number | null;
  invoicedSum?: number | null;
  notYetInvoicedSum?: number | null;
  /**
   * The probability the forecast applies to this position, as a factor between 0 and 1: it follows from the
   * status of the order *and* of the position, so the order's `probabilityOfOccurrence` field is only what
   * it falls back to (`ForecastUtils.getProbabilityOfAccurence`).
   */
  probabilityOfOccurrence?: number | null;
}

/** What `AuftragPagesRest.recalculate` answers (`OrderSums` there). */
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
  toBeInvoiced: boolean;
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
 * otherwise. It cannot be hidden beforehand — `UserStatus.systemData` carries no development flag — so
 * the caller reports the failure instead of the button being absent.
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
  saveBlob(await res.blob(), `forecast-order-${id}.json`);
}
