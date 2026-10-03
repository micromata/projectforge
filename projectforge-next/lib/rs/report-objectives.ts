/**
 * The calls of the report objectives page (`ReportObjectivesPageRest`): upload of a ReportObjective XML,
 * its evaluation over a month range, the navigation through its child reports and the reset.
 *
 * Every call answers the page's whole new state. A refusal (no XML, unreadable file, invalid period) is an
 * HTTP 400 whose body is the backend's translated text, thrown as an [RsError] carrying exactly that.
 */

import { rawRequest, RsError } from "./client";
import { uploadWithProgress } from "./upload";
import type { ReportObjectivesData } from "@/components/features/report-objectives/types";

const BASE = "/rs/reportObjectives";

export const REPORT_OBJECTIVES_QUERY_KEY = ["reportObjectives"] as const;

async function call(
  path: string,
  init: RequestInit,
  signal?: AbortSignal
): Promise<ReportObjectivesData> {
  const res = await rawRequest(path, init, signal);
  if (!res.ok) {
    const text = res.status === 400 ? await res.text().catch(() => "") : "";
    throw new RsError(
      res.status,
      text || `${res.status} ${res.statusText}: ${path}`
    );
  }
  return (await res.json()) as ReportObjectivesData;
}

export function fetchReportObjectives(
  signal?: AbortSignal
): Promise<ReportObjectivesData> {
  return call(BASE, { method: "GET" }, signal);
}

/** Stores the report objectives of the given XML file, replacing any stored ones. */
export async function uploadReportObjectives(
  file: File
): Promise<ReportObjectivesData> {
  const body = new FormData();
  body.append("file", file);
  const path = `${BASE}/upload`;
  const res = await uploadWithProgress(path, body);
  if (res.status < 200 || res.status >= 300) {
    throw new RsError(res.status, res.text || `${res.status}: ${path}`);
  }
  return JSON.parse(res.text) as ReportObjectivesData;
}

/** As [uploadReportObjectives], but with the XML pasted into the page. */
export function pasteReportObjectives(
  xml: string
): Promise<ReportObjectivesData> {
  return call(`${BASE}/paste`, {
    method: "POST",
    body: JSON.stringify({ xml }),
  });
}

/** Selects the accounting records of the given months (`yyyy-MM`) for the stored report objectives. */
export function createReport(
  fromMonth: string,
  toMonth: string | null
): Promise<ReportObjectivesData> {
  return call(`${BASE}/create`, {
    method: "POST",
    body: JSON.stringify({ fromMonth, toMonth }),
  });
}

/** Makes the given report (an ancestor or a child of the current one) the current report. */
export function selectReport(reportId: string): Promise<ReportObjectivesData> {
  return call(`${BASE}/select`, {
    method: "POST",
    body: JSON.stringify({ reportId }),
  });
}

/** Removes the stored report objectives. */
export function clearReportObjectives(): Promise<ReportObjectivesData> {
  return call(`${BASE}/clear`, { method: "POST" });
}
