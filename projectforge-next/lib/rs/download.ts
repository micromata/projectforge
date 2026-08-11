/**
 * Hands a response body to the browser as a download.
 *
 * Shared by every endpoint that may answer with a file instead of JSON: the actions of the dynamic
 * pages (see ./dynamic.ts), and the forecast export of the order book (./order.ts). The subject is the
 * same in both cases — a `Blob` is already in memory and has to become a saved file — while the
 * protocol around it differs, which is why this is a helper of its own rather than part of either.
 */

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
