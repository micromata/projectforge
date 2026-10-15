/**
 * Filename of a download, taken from its `Content-Disposition` header.
 *
 * Spring sends both forms, e.g. `attachment; filename*=UTF-8''document.pdf; filename=document.pdf`
 * (see RestUtils.setContentDisposition). The RFC 5987 `filename*` wins because it is the one that
 * survives non-ASCII names; `filename` is the fallback for older clients.
 *
 * Port of `Object.getResponseHeaderFilename` in projectforge-webapp/src/utilities/global.js, which
 * used a single regex that happened to match whichever form came last.
 */
export function parseContentDispositionFilename(
  contentDisposition: string | null | undefined
): string {
  if (!contentDisposition) return "download";

  const extended = /filename\*\s*=\s*(?:UTF-8|utf-8)''([^;]+)/.exec(
    contentDisposition
  );
  if (extended?.[1]) {
    return decodeFilename(extended[1]) ?? "download";
  }

  const plain = /filename\s*=\s*("([^"]*)"|[^;]*)/.exec(contentDisposition);
  const raw = plain?.[2] ?? plain?.[1];
  return raw ? (decodeFilename(raw) ?? "download") : "download";
}

/** Percent-decodes and strips path segments, so a hostile name cannot escape the download dir. */
function decodeFilename(raw: string): string | null {
  let value = raw.trim().replace(/^["']|["']$/g, "");
  try {
    value = decodeURIComponent(value);
  } catch {
    // Keep the raw value: a stray "%" is more likely than an actual encoding.
  }
  value = value.split(/[/\\]/).pop() ?? "";
  return value.length > 0 ? value : null;
}
