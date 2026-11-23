import { cn } from "@/lib/utils";

/**
 * The look of numbers in table cells, defined once so every table — declared lists, hand-built tables,
 * the import preview — lays them out the same.
 *
 * - numeric (amounts, decimals, whole numbers): monospaced with tabular digits; the column is right-aligned
 *   through its `align`.
 * - cost (cost unit numbers such as `5.100.01.02`): monospaced, so digits and dots line up; left-aligned.
 */
export const NUMERIC_CELL_CLASS = "font-mono tabular-nums";
export const COST_CELL_CLASS = "font-mono";

export type NumberCellKind = "numeric" | "cost";

/** The classes of a number cell of the given kind, plus any extra classes. */
export function cellClassFor(
  kind: NumberCellKind,
  ...extra: (string | false | null | undefined)[]
): string {
  return cn(kind === "cost" ? COST_CELL_CLASS : NUMERIC_CELL_CLASS, ...extra);
}
