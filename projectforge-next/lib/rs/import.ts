/**
 * The generic, layout-free CSV/DATEV import endpoints (`AbstractImportRest`), parameterised by the REST
 * path base. Every concrete import (incoming invoice first, address and banking later) binds its base and
 * reuses these — the parallel of list-export.ts, which the concrete invoice clients wrap the same way.
 *
 * The endpoints answer PLAIN JSON (an `ImportView`, `{ jobId }` or `{ error }`), not the UILayout
 * `ResponseAction` protocol — so nothing here goes through the dynamic action interpreter.
 */

import { rawRequest, request, RsError } from "./client";
import { uploadWithProgress, type UploadOptions } from "./upload";
import type {
  DisplayOptions,
  ImportView,
} from "@/components/shared/import/import-types";

/** What `POST upload` answers: the parsed view, or the (already translated) reason it was refused. */
export type UploadImportResult =
  | { kind: "ok"; view: ImportView }
  | { kind: "rejected"; error: string };

/**
 * Uploads the file and, on success, answers its parsed [ImportView]. A refusal (empty file, wrong format,
 * parse error) is a `400` carrying `{ error }`, which is returned rather than thrown — it is a message for
 * the user, not a fault. Progress and abort go through [uploadWithProgress] (XHR); see lib/rs/upload.ts.
 */
export async function uploadImportFile(
  base: string,
  file: File,
  options: UploadOptions = {}
): Promise<UploadImportResult> {
  const body = new FormData();
  body.append("file", file);
  const path = `/rs/${base}/upload`;
  const res = await uploadWithProgress(path, body, options);
  const parsed = parseJson(res.text);
  if (res.status >= 200 && res.status < 300) {
    return { kind: "ok", view: parsed as ImportView };
  }
  return {
    kind: "rejected",
    error: (parsed as { error?: string } | null)?.error ?? `${res.status}`,
  };
}

/** The current [ImportView] from the session, or an empty one (`{ hasBeenReconciled, entries }`). */
export function fetchImportState(
  base: string,
  signal?: AbortSignal
): Promise<ImportView> {
  return request<ImportView>(`/rs/${base}/state`, { method: "GET" }, signal);
}

/** Reconciles the stashed import with the database and answers the refreshed [ImportView]. */
export function reconcileImport(
  base: string,
  displayOptions?: DisplayOptions,
  signal?: AbortSignal
): Promise<ImportView> {
  return request<ImportView>(
    `/rs/${base}/reconcile`,
    { method: "POST", body: JSON.stringify(displayOptions ?? {}) },
    signal
  );
}

/** The id of the background job a commit enqueues. */
export interface CommitImportResult {
  jobId: number;
}

/**
 * Commits the ticked entries and answers the id of the import job. A refusal (nothing importable selected,
 * not reconciled) is a `400`/`500` carrying `{ error }`; its message is thrown so the caller can show it —
 * which is why this uses [rawRequest] rather than [request], whose thrown error would lose the body.
 */
export async function commitImport(
  base: string,
  selectedIds: number[],
  displayOptions?: DisplayOptions,
  signal?: AbortSignal
): Promise<CommitImportResult> {
  const path = `/rs/${base}/commit`;
  const res = await rawRequest(
    path,
    { method: "POST", body: JSON.stringify({ selectedIds, displayOptions }) },
    signal
  );
  const text = await res.text();
  if (!res.ok) {
    const error = (parseJson(text) as { error?: string } | null)?.error;
    throw new RsError(res.status, error ?? `${res.status}: ${path}`);
  }
  return JSON.parse(text) as CommitImportResult;
}

/** Drops the stashed import from the session. Answers an empty body, so nothing is parsed. */
export async function cancelImport(
  base: string,
  signal?: AbortSignal
): Promise<void> {
  const path = `/rs/${base}/cancel`;
  const res = await rawRequest(path, { method: "POST" }, signal);
  if (!res.ok) throw new RsError(res.status, `${res.status}: ${path}`);
}

/** Parses a JSON body, tolerating an empty or non-JSON one (returns null then). */
function parseJson(text: string): unknown {
  try {
    return text ? JSON.parse(text) : null;
  } catch {
    return null;
  }
}
