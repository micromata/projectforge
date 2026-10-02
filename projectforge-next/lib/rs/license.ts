/**
 * The calls of the license page besides the standard REST ones (`LicenseEntityRest`): the suggestions of
 * its text fields, and the two stored files (`/rs/license/file/…`) — the slots Wicket's `LicenseEditForm`
 * offered as "file 1" and "file 2", e.g. the license document and the invoice.
 *
 * The files are no attachments: the license stores them as two BLOBs of its own, each with a name, so the
 * calls here are read/replace/remove of a slot plus the swap of both, not the add/rename/delete of a list
 * (see ./attachments.ts for that). Only for a user who may see the license's key; anyone else is refused.
 */

import { request, RsError } from "./client";
import { downloadFile } from "./download";
import { fetchAutoCompletion } from "./dynamic";
import { uploadWithProgress, type UploadOptions } from "./upload";

/** One of the two slots. */
export type LicenseFileSlot = 1 | 2;

/** `LicenseEntityRest.LicenseFilesState` — a name is absent where its slot holds no file. */
export interface LicenseFilesState {
  filename1?: string | null;
  filename2?: string | null;
}

const BASE = "/rs/license/file";

export function licenseFilesQueryKey(licenseId: number | null) {
  return ["license", "files", licenseId] as const;
}

export function fetchLicenseFiles(
  licenseId: number,
  signal?: AbortSignal
): Promise<LicenseFilesState> {
  return request<LicenseFilesState>(
    `${BASE}/${licenseId}`,
    { method: "GET" },
    signal
  );
}

/** Downloads the file of a slot; the backend names it (`RestUtils.downloadFile`). */
export function downloadLicenseFile(
  licenseId: number,
  slot: LicenseFileSlot
): Promise<void> {
  return downloadFile(`${BASE}/${licenseId}/${slot}`);
}

/**
 * Stores a file in a slot, replacing whatever was there, and answers the new state.
 *
 * A refusal (too large) is an HTTP 400 whose body is the backend's own translated text (`FileCheck`), so
 * it is thrown as an [RsError] carrying exactly that.
 */
export async function uploadLicenseFile(
  licenseId: number,
  slot: LicenseFileSlot,
  file: File,
  options: UploadOptions = {}
): Promise<LicenseFilesState> {
  const body = new FormData();
  body.append("file", file);
  const path = `${BASE}/${licenseId}/${slot}`;
  const res = await uploadWithProgress(path, body, options);
  if (res.status < 200 || res.status >= 300) {
    throw new RsError(res.status, res.text || `${res.status}: ${path}`);
  }
  return JSON.parse(res.text) as LicenseFilesState;
}

export function deleteLicenseFile(
  licenseId: number,
  slot: LicenseFileSlot
): Promise<LicenseFilesState> {
  return request<LicenseFilesState>(`${BASE}/${licenseId}/${slot}`, {
    method: "DELETE",
  });
}

/** Swaps the two files, names included. */
export function swapLicenseFiles(
  licenseId: number
): Promise<LicenseFilesState> {
  return request<LicenseFilesState>(`${BASE}/${licenseId}/swap`, {
    method: "POST",
  });
}

/** The values the backend already has for a text field of a license (`LicenseDao` opts them in). */
export function fetchLicenseSuggestions(
  property: "organization" | "product",
  search: string,
  signal?: AbortSignal
): Promise<string[]> {
  return fetchAutoCompletion<string>(
    `license/autocomplete?property=${property}&search=:search`,
    search,
    undefined,
    signal
  );
}
