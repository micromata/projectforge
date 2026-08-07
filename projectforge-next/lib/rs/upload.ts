/**
 * The one request in this app that does not go through `rawRequest`: a file upload that reports its
 * progress.
 *
 * `fetch` cannot do that. It has no upload-progress event at all, and the streaming request bodies
 * that would allow counting bytes yourself require HTTP/2 and are unsupported in Safari and Firefox.
 * `XMLHttpRequest.upload.onprogress` is the only portable way — which is why the legacy frontend
 * used XHR here too (`SingleFileUploadWithProgress.tsx`).
 *
 * What is lost by leaving `rawRequest`: the transparent 2FA and CSRF-token retries. That is
 * acceptable for exactly this call — a retry would mean sending the whole file a second time, and
 * the token is fetched at app start, so a stale one means the session is gone and the reload after
 * re-login repeats the upload anyway. Everything else (cookie, CSRF header, `X-PF-Frontend`) is
 * mirrored below.
 */

import { getCsrfToken } from "./client";

export interface UploadProgress {
  /** Bytes sent so far. */
  loaded: number;
  /** Total bytes, or null while the browser cannot tell (`lengthComputable` false). */
  total: number | null;
  /** 0-100, rounded; null while the total is unknown. */
  percent: number | null;
}

export interface UploadOptions {
  onProgress?: (progress: UploadProgress) => void;
  /** Aborts the transfer — for the cancel button of a running upload. */
  signal?: AbortSignal;
}

export interface UploadResponse {
  status: number;
  /** Raw body; the caller parses it, since only it knows the protocol. */
  text: string;
}

/** Thrown when the transfer itself failed (network, abort) — as opposed to an HTTP error status. */
export class UploadError extends Error {
  constructor(
    message: string,
    /** True if the caller aborted it, so a UI can stay quiet about it. */
    public readonly aborted = false
  ) {
    super(message);
    this.name = "UploadError";
  }
}

/**
 * POSTs a multipart body and reports progress while it goes out.
 *
 * Resolves for **every** HTTP status, error statuses included: the attachment protocol answers a
 * refusal with 200 and a TOAST, so the status is data for the caller rather than a reason to throw.
 * Only a failed transfer rejects.
 */
export function uploadWithProgress(
  path: string,
  body: FormData,
  { onProgress, signal }: UploadOptions = {}
): Promise<UploadResponse> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(new UploadError("aborted", true));
      return;
    }
    const xhr = new XMLHttpRequest();
    xhr.open("POST", path);
    // Same as fetch's `credentials: "include"` — the session lives in the JSESSIONID cookie.
    xhr.withCredentials = true;
    // No Content-Type: the browser derives it from the FormData, boundary included.
    xhr.setRequestHeader("X-PF-Frontend", "next");
    const token = getCsrfToken();
    if (token) xhr.setRequestHeader("X-PF-CSRF-Token", token);

    const onAbort = () => xhr.abort();
    signal?.addEventListener("abort", onAbort);
    const done = () => signal?.removeEventListener("abort", onAbort);

    if (onProgress) {
      xhr.upload.onprogress = (event) => {
        const total = event.lengthComputable ? event.total : null;
        onProgress({
          loaded: event.loaded,
          total,
          percent: total ? Math.round((event.loaded / total) * 100) : null,
        });
      };
    }
    xhr.onload = () => {
      done();
      resolve({ status: xhr.status, text: xhr.responseText });
    };
    xhr.onerror = () => {
      done();
      // XHR deliberately withholds the cause of a network error (it would leak cross-origin
      // information), so there is nothing more specific to report here.
      reject(new UploadError("network error"));
    };
    xhr.onabort = () => {
      done();
      reject(new UploadError("aborted", true));
    };
    xhr.ontimeout = () => {
      done();
      reject(new UploadError("timeout"));
    };

    xhr.send(body);
  });
}
