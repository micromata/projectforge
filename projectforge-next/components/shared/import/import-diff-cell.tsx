"use client";

import { cn } from "@/lib/utils";
import { useFormatContext } from "@/hooks/use-format";
import { diffOf, formatByKind } from "./import-model";
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
 */
export function ImportDiffCell({ entry, column }: Props) {
  const ctx = useFormatContext();
  const { current, old, hasDiff } = diffOf(entry, column);
  const currentText = formatByKind(current, column.kind, ctx);

  if (!column.diff || !hasDiff) {
    return <span className="block truncate">{currentText}</span>;
  }

  const oldText = formatByKind(old, column.kind, ctx);
  return (
    <span className="flex flex-col leading-tight">
      <span className={cn("truncate text-brand-pink line-through")}>
        {oldText}
      </span>
      <span className="truncate text-brand-green-dark">{currentText}</span>
    </span>
  );
}
