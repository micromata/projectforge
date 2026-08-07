/**
 * `valueGetter` and `valueFormatter` arrive as AG-Grid expressions, i.e. as
 * JavaScript source. We never execute server-supplied code, so both are parsed
 * strictly as a dot path and anything else is discarded.
 *
 * That loses nothing in practice: every real `valueFormatter` says "read this
 * pre-rendered sibling field" (`data.sizeHumanReadable`, `data.timeAgo`, …), and
 * every real `valueGetter` addresses a nested property. The one JavaScript
 * one-liner in the backend (the address book column) is unreachable, because that
 * column also sets `cellRenderer: "formatter"`, which wins.
 */

const DOT_PATH = /^[A-Za-z_$][\w$]*(\.[A-Za-z_$][\w$]*)*$/;

/**
 * Turns an AG-Grid expression into a dot path relative to the row, or returns
 * undefined if it is anything more than that.
 */
export function parseValuePath(
  expression: string | undefined
): string | undefined {
  if (!expression) return undefined;
  const path = expression
    .trim()
    // AG-Grid addresses the row as `data`; our paths are relative to the row.
    .replace(/^data\??\./, "")
    .replace(/\?\./g, ".");
  if (!DOT_PATH.test(path)) {
    warnUnsupported(expression);
    return undefined;
  }
  return path;
}

function warnUnsupported(expression: string): void {
  if (process.env.NODE_ENV !== "production") {
    console.warn(
      `[dynamic-grid] Ignoring non-path AG-Grid expression: ${expression}`
    );
  }
}
