/**
 * How an operation type is shown: its colour and its symbol.
 *
 * Shared by the entry header (which colours the `diffSummary` counts) and the attribute rows, so
 * both speak the same visual language. The parameter is a plain string because `EntityOpType` and
 * `PropertyOpType` overlap but are not the same set — the latter adds `Undefined`, the former
 * `MarkAsDeleted`/`Undelete`, and neither has a colour of its own.
 */

/** CSS var holding the colour of an operation, see globals.css. */
export function opColor(type: string | null): string {
  if (type === "Insert") return "var(--history-insert)";
  if (type === "Delete") return "var(--history-delete)";
  return "var(--history-update)";
}

/** Legacy's prefix for a changed property: `+` added, `~` changed, `-` removed. */
export function opSymbol(type: string | null): string {
  if (type === "Insert") return "+";
  if (type === "Update") return "~";
  if (type === "Delete") return "-";
  return "";
}
