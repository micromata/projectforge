/**
 * The Excel export every list page can have: `POST /rs/{entity}/exportAsExcel` with the filter the list
 * is showing (`RestPaths.REST_EXCEL_SUB_PATH`).
 *
 * One function rather than one per entity, because the contract is the same for all of them: the backend
 * runs the query the list ran and writes its whole result set, so the export needs nothing but the filter.
 * Which lists offer it is the frontend's decision (see PageDef.listActions) — the endpoint exists per
 * `*PagesRest` that implements it, and each of them checks the rights itself
 * (`GroupPagesRest.exportAsExcel` requires an administrator).
 */

import { downloadPost } from "./download";
import type { MagicFilter } from "./types";

/**
 * The filtered entries of a list as an Excel file, saved under the name the backend gave it.
 *
 * A 404 means the filter matched nothing; the callers say so instead of reporting an error (see
 * `GroupListActions` and `InvoiceListActions`).
 */
export function downloadListExcel(
  entity: string,
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<void> {
  return downloadPost(`/rs/${entity}/exportAsExcel`, filter, signal);
}
