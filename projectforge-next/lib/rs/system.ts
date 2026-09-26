/**
 * The System (administration) page (`org.projectforge.rest.SystemRest`), successor of Wicket's
 * `wa/admin`. A non-entity, standalone action page, so it has its own small client here rather than
 * going through `fetchList` / the entity plumbing.
 *
 * Every endpoint is admin-only and self-checks on the backend (there is no DAO access backstop); the
 * message actions answer with a translated `{ message }`, the export actions with a file, and the
 * reindex with the id of a background job to poll (see ./jobs.ts).
 */

import { request } from "./client";
import { downloadFile } from "./download";
import type { SystemAdminData } from "@/components/features/system/types";

/** The result of a message-returning action: a text already translated by the backend. */
export interface SystemMessageResponse {
  message: string;
}

export function fetchSystemAdminData(
  signal?: AbortSignal
): Promise<SystemAdminData> {
  return request<SystemAdminData>("/rs/system", { method: "GET" }, signal);
}

function postMessage(action: string): Promise<SystemMessageResponse> {
  return request<SystemMessageResponse>(`/rs/system/${action}`, {
    method: "POST",
  });
}

export const refreshCaches = () => postMessage("refreshCaches");
export const rereadConfiguration = () => postMessage("rereadConfiguration");
export const createMissingIndices = () => postMessage("createMissingIndices");
export const resetIdpPasswordSync = () => postMessage("resetIdpPasswordSync");
export const testDatabase = () => postMessage("testDatabase");
export const createTestBooks = () => postMessage("createTestBooks");
export const clearAlertMessage = () => postMessage("clearAlertMessage");

export function setAlertMessage(
  alertMessage: string
): Promise<SystemMessageResponse> {
  return request<SystemMessageResponse>("/rs/system/setAlertMessage", {
    method: "POST",
    body: JSON.stringify({ alertMessage }),
  });
}

// The export actions answer with a file; the filename comes from the Content-Disposition header the
// backend sends (RestUtils.downloadFile), so it is never guessed here (see ./download.ts).
export const downloadExportConfiguration = () =>
  downloadFile("/rs/system/exportConfiguration");
export const downloadExport2FAConfiguration = () =>
  downloadFile("/rs/system/export2FAConfiguration");
export const downloadCheckSystemIntegrity = () =>
  downloadFile("/rs/system/checkSystemIntegrity");
export const downloadExportSchema = () =>
  downloadFile("/rs/system/exportSchema");
export const downloadOptimizeAddressImages = () =>
  downloadFile("/rs/system/optimizeAddressImages");
export const downloadCheckI18nProperties = () =>
  downloadFile("/rs/system/checkI18nProperties");
export const downloadDebugUserGroupCache = () =>
  downloadFile("/rs/system/debugUserGroupCache");

/** Body of a reindex run: an optional entry limit and an optional "modified since" date (yyyy-MM-dd). */
export interface ReindexRequestBody {
  newestNEntries?: number | null;
  fromDate?: string | null;
}

/** Starts a reindex run through the background-job infrastructure; answers with the job id to poll. */
export function startReindex(
  body: ReindexRequestBody,
  signal?: AbortSignal
): Promise<{ jobId: number }> {
  return request<{ jobId: number }>(
    "/rs/system/reindex",
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
}
