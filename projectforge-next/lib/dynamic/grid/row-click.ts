import type { ActionDef } from "@/lib/rs/types";
import type { DataObject } from "@/lib/dynamic/path";
import type { AgGridNode } from "./ag-grid-types";

/**
 * Turns a grid's row-click configuration into a synthetic `ActionDef`, so the
 * click runs through the existing `callAction` interpreter instead of navigating
 * on its own. That way a row click gets `sanitizeRedirectUrl`/`resolveMenuUrl`,
 * the depth limit and the toast handling for free.
 *
 * `rowClickOpenModal` is not honoured: next has no modal layer yet, so the page
 * the url points at is opened instead (see MIGRATION.md).
 */
export function rowClickTargetFor(
  grid: AgGridNode,
  row: DataObject
): ActionDef | undefined {
  const id = "rowClick";

  if (grid.rowClickRedirectUrl) {
    return {
      id,
      responseAction: {
        targetType: "REDIRECT",
        url: applyRowId(grid.rowClickRedirectUrl, row),
      },
    };
  }

  if (grid.rowClickPostUrl) {
    const rowId = row.id;
    if (rowId == null) return undefined;
    return {
      id,
      responseAction: {
        targetType: "POST",
        // The legacy client appended the id the same way; the endpoints expect it
        // as a path segment, not as a body field.
        url: `${grid.rowClickPostUrl}/${rowId}`,
      },
    };
  }

  if (grid.rowClickFunction && process.env.NODE_ENV !== "production") {
    console.warn(
      `[dynamic-grid] grid "${grid.id}" sends rowClickFunction (JavaScript source); ignored, rows are not clickable.`
    );
  }
  return undefined;
}

/** `{id}` / `:id` / a trailing `/id` path segment — the three forms senders use. */
const ID_PLACEHOLDER = /\{id}|:id|(?<=\/)id(?=$|[/?#])/g;

/**
 * Substitutes the row's id into a redirect template.
 *
 * Deliberately narrower than the legacy `modifyRedirectUrl`, which substituted
 * *every* row field into path and query placeholders — an unbounded template
 * language over server data. Only the id is resolved here; any other placeholder
 * survives untouched and is reported, since the pages using those (the importers)
 * are not migrated yet.
 */
export function applyRowId(url: string, row: DataObject): string {
  const id = row.id;
  const resolved =
    id == null
      ? url
      : url.replace(ID_PLACEHOLDER, encodeURIComponent(String(id)));
  if (/\{\w+}/.test(resolved) && process.env.NODE_ENV !== "production") {
    console.warn(
      `[dynamic-grid] unresolved placeholder in row click url "${resolved}"; only {id} is substituted.`
    );
  }
  return resolved;
}
