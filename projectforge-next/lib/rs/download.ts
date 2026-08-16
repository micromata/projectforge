/**
 * Fetching a file from the backend and handing it to the browser as a download.
 *
 * Shared by every endpoint that answers with a file instead of JSON: the actions of the dynamic pages
 * (see ./dynamic.ts), the exports of the order book and the invoice list, and the protocol of a mass
 * update run.
 */

import { rawRequest, RsError } from "./client";
import { parseContentDispositionFilename } from "@/lib/dynamic/content-disposition";

/**
 * @param filename What the file is saved as. The callers take it from the `Content-Disposition` header
 * where the backend sends one (see lib/dynamic/content-disposition.ts) and name it themselves where it
 * doesn't — never guessed from the url, which carries an id, not a name.
 */
export function saveBlob(blob: Blob, filename: string): void {
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  // Chrome needs the url alive until the click was processed.
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
}

/**
 * Fetches a file and saves it under the name the backend gave it.
 *
 * The name comes from `Content-Disposition`, which every file answering endpoint sends
 * (`RestUtils.downloadFile`) — so it is the backend that names the file, not a guess from the url.
 *
 * A fetch rather than a plain link, for two reasons that apply to all callers: an export POSTs the
 * filter as its body, and a failure answers a status the caller has to read (a 404 means the filter
 * matched nothing, an expired download slot answers 400) instead of replacing the page with an error.
 * The status is passed on as [RsError] for exactly that.
 */
export async function downloadFile(
  path: string,
  init?: RequestInit,
  signal?: AbortSignal
): Promise<void> {
  const res = await rawRequest(path, init ?? { method: "GET" }, signal);
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  saveBlob(
    await res.blob(),
    parseContentDispositionFilename(res.headers.get("Content-Disposition"))
  );
}

/** [downloadFile] with a JSON body — an export acting on the filter the list is showing. */
export function downloadPost(
  path: string,
  body: unknown,
  signal?: AbortSignal
): Promise<void> {
  return downloadFile(
    path,
    { method: "POST", body: JSON.stringify(body) },
    signal
  );
}
