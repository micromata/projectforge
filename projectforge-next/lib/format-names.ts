import type { CellKind } from "@/components/data-table/cells/cell-types";

/**
 * The formatter names the backend sends in a column def
 * (UIAgGridColumnDef.Formatter, delivered as cellRendererParams.dataType).
 *
 * BOOLEAN, RATING, CONSUMPTION and TREE_NAVIGATION are part of the union but not
 * of [formatValue]: they render icons or a progress bar rather than text, so a
 * cell component handles them (see [formatterToCellKind]).
 */
export type FormatterName =
  | "BOOLEAN"
  | "CONSUMPTION"
  | "CURRENCY"
  | "CURRENCY_PLAIN"
  | "DATE"
  | "NUMBER"
  | "TIMESTAMP_MINUTES"
  | "TIMESTAMP_SECONDS"
  | "PERCENTAGE"
  | "PERCENTAGE_DECIMAL"
  | "RATING"
  | "SHOW_DISPLAYNAME"
  | "SHOW_LIST_OF_DISPLAYNAMES"
  | "TREE_NAVIGATION"
  | "ADDRESS_BOOK"
  | "AUFTRAG_POSITION"
  | "EMPLOYEE"
  | "COST1"
  | "COST2"
  | "CUSTOMER"
  | "GROUP"
  | "KONTO"
  | "PROJECT"
  | "TASK_PATH"
  | "USER"
  | "ORDERS"
  | "TASK_STATUS";

/**
 * Spellings that are not in the enum but reach the client anyway, mapped onto
 * the name that behaves identically. `AMOUNT` comes from hand-written column
 * defs; `TIMESTAMP`/`TASK`/`AUFTRAGPOSITION` are the legacy webapp's own
 * spellings, kept so a column def copied from there keeps working.
 */
const FORMATTER_ALIASES: Record<string, FormatterName> = {
  AMOUNT: "CURRENCY_PLAIN",
  TIMESTAMP: "TIMESTAMP_SECONDS",
  TASK: "TASK_PATH",
  AUFTRAGPOSITION: "AUFTRAG_POSITION",
};

/** Resolves an alias; anything unknown is passed through unchanged. */
export function canonicalFormatter(
  name: string | undefined
): FormatterName | undefined {
  if (!name) return undefined;
  return FORMATTER_ALIASES[name] ?? (name as FormatterName);
}

/** The formatters that need their own cell component rather than plain text. */
const CELL_KINDS: Partial<Record<FormatterName, CellKind>> = {
  BOOLEAN: "boolean",
  RATING: "rating",
  CONSUMPTION: "consumption",
  TREE_NAVIGATION: "tree",
  ORDERS: "orders",
  TASK_STATUS: "taskStatus",
};

/** Which cell component renders this formatter. Everything else is text. */
export function formatterToCellKind(name: string | undefined): CellKind {
  const canonical = canonicalFormatter(name);
  return (canonical && CELL_KINDS[canonical]) ?? "text";
}
