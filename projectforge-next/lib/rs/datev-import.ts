/**
 * The two REST bases of the DATEV import, both fed by the same original file of the tax office: the accounting
 * records (`DatevRecordImportRest`) and the chart of accounts (`DatevAccountImportRest`). The generic import
 * client (./import.ts) is addressed by these bases directly.
 */

import { request } from "./client";

/** REST path base of the accounting record import. */
export const RECORD_ENTITY = "datevRecordImport";

/** REST path base of the chart of accounts import. */
export const ACCOUNT_ENTITY = "datevAccountImport";

/**
 * The log viewer url of the user's DATEV import log (both tabs), relative to the app root, e.g.
 * `next/logViewer/7`; `null` without a logged-in user. See DatevImportLog on the backend.
 */
export async function fetchDatevImportLogViewerUrl(
  signal?: AbortSignal
): Promise<string | null> {
  const { url } = await request<{ url: string | null }>(
    `/rs/${RECORD_ENTITY}/logViewer`,
    { method: "GET" },
    signal
  );
  return url;
}
