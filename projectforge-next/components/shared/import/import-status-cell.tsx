"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Alert02Icon } from "@hugeicons/core-free-icons";
import { leafKeyOf } from "@/lib/leaf-key";
import { cn } from "@/lib/utils";
import { useFormatContext } from "@/hooks/use-format";
import { formatByKind, type HiddenChange } from "./import-model";
import type { ImportEntry } from "./import-types";

interface Props {
  entry: ImportEntry;
  /** The changes of the row no visible column shows (see changesNotShownOf). */
  hiddenChanges?: HiddenChange[];
}

/**
 * The reconciliation state of a row, already localised by the backend (`statusAsString`). A faulty row
 * (`hasError`) leads with an alert icon whose label carries the reason, so the message is reachable
 * without a column of its own.
 *
 * A modified row whose changes aren't all visible names the hidden ones in a second line (e.g. the paid date of
 * a position-based file, whose column isn't shown), with their old → new values in the tooltip.
 *
 * The reason is declared to the table's own tooltip (`data-tooltip`, the delegated
 * {@link useOverflowTooltip}) rather than as a native `title`: one styled tooltip for the whole grid,
 * announced by screen readers, and openable by tap on a phone — a native `title` is none of these.
 */
export function ImportStatusCell({ entry, hiddenChanges = [] }: Props) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const changes = hiddenChanges.map((change) => {
    const label = change.column
      ? t(leafKeyOf(change.column.headerKey, t.has))
      : change.field;
    const kind = change.column?.kind ?? "text";
    const format = (value: unknown) => formatByKind(value, kind, ctx) || "–";
    return {
      label,
      line: `${label}: ${format(change.old)} → ${format(change.current)}`,
    };
  });
  const tooltip =
    [entry.error, ...changes.map((change) => change.line)]
      .filter(Boolean)
      .join("\n") || undefined;
  return (
    <span className="flex flex-col leading-tight" data-tooltip={tooltip}>
      <span className={cn("flex items-center gap-1.5")}>
        {entry.hasError && (
          <HugeiconsIcon
            icon={Alert02Icon}
            size={16}
            className="shrink-0 text-destructive"
            aria-label={entry.error ?? "error"}
          />
        )}
        <span className="truncate">{entry.statusAsString}</span>
      </span>
      {changes.length > 0 && (
        <span className="truncate text-xs text-muted-foreground">
          {changes.map((change) => change.label).join(", ")}
        </span>
      )}
    </span>
  );
}
