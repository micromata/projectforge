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
 * The body of a file answer as a blob, for [saveBlob].
 *
 * Reads it as an `ArrayBuffer` rather than calling `res.blob()`, which is the point of this function:
 * a blob taken from a POST response makes Chromium request the url a **second** time to back it — as a
 * bodyless GET, which Spring routes to `/rs/{entity}/{id}` and answers with a stack trace ("For input
 * string: exportAsExcel"). The file arrived either way; the second request only filled the server log.
 *
 * The content type is carried over, since it is what tells the browser what it is saving.
 */
export async function responseBlob(res: Response): Promise<Blob> {
  const buffer = await res.arrayBuffer();
  return new Blob([buffer], {
    type: res.headers.get("Content-Type") ?? "application/octet-stream",
  });
}

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
    // A refusal explains itself in the body, and every caller of this puts the message into a toast —
    // where the status line would otherwise show the user a rest url. Two shapes of body, because the
    // backend has two ways of refusing: a `RestError` object (the translated text of an AccessException),
    // and a plain string where the endpoint answers `badRequest().body(text)` — the e-invoice exports do
    // that with the list of what is missing on the invoice.
    const text = await res.text().catch(() => "");
    const message = parseErrorBody(text);
    throw new RsError(
      res.status,
      message || `${res.status} ${res.statusText}: ${path}`
    );
  }
  saveBlob(
    await responseBlob(res),
    parseContentDispositionFilename(res.headers.get("Content-Disposition"))
  );
}

/**
 * The message of a refused download, whichever of the two shapes its body has.
 *
 * A JSON object's `message` (Spring's own error body and `RestError`), otherwise the body as it is — the
 * text an endpoint wrote itself. An empty answer yields an empty string, and the caller falls back to the
 * status.
 */
function parseErrorBody(text: string): string {
  const trimmed = text.trim();
  if (!trimmed.startsWith("{")) return trimmed;
  try {
    const body = JSON.parse(trimmed) as { message?: string };
    return body.message ?? trimmed;
  } catch {
    return trimmed;
  }
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
