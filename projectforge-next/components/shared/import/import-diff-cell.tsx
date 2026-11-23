"use client";

import { cn } from "@/lib/utils";
import { cellClassFor } from "@/lib/cell-styles";
import { useFormatContext } from "@/hooks/use-format";
import { diffOf, formatByKind, isNumericKind, tooltipOf } from "./import-model";
import type { ImportColumn, ImportEntry } from "./import-types";

interface Props {
  entry: ImportEntry;
  column: ImportColumn;
}

/**
 * One preview cell. A plain formatted value, except on a MODIFIED row whose property changed: then the
 * old (stored) value is shown struck through in pink above the new one in green — the two-line diff of
 * the legacy import grid, read from `oldDiffValues["read." + field]` (see diffOf).
 *
 * A column that opted out of diffs (`column.diff !== true`) never splits, even on a changed row: the
 * value is the same everywhere and the second line would be noise.
 *
 * A column's `tooltipField` is declared to the table's own tooltip (`data-tooltip`, the delegated
 * {@link useOverflowTooltip}), as the status cell does — not one tooltip root per cell.
 */
export function ImportDiffCell({ entry, column }: Props) {
  const ctx = useFormatContext();
  const { current, old, hasDiff } = diffOf(entry, column);
  const currentText = formatByKind(current, column.kind, ctx);
  // Amounts, year and month read as monospaced digits and cost units monospaced, as in every other table
  // (see lib/cell-styles).
  const numberClass = isNumericKind(column.kind)
    ? cellClassFor("numeric")
    : column.kind === "cost"
      ? cellClassFor("cost")
      : undefined;
  const tooltip = tooltipOf(entry, column);

  if (!column.diff || !hasDiff) {
    return (
      <span
        className={cn("block truncate", numberClass)}
        data-tooltip={tooltip}
      >
        {currentText}
      </span>
    );
  }

  const oldText = formatByKind(old, column.kind, ctx);
  return (
    <span className="flex flex-col leading-tight" data-tooltip={tooltip}>
      <span
        className={cn("truncate text-brand-pink line-through", numberClass)}
      >
        {oldText}
      </span>
      <span className={cn("truncate text-brand-green-dark", numberClass)}>
        {currentText}
      </span>
    </span>
  );
}
