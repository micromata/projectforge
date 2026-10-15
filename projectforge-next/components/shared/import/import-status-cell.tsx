"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { Alert02Icon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";
import type { ImportEntry } from "./import-types";

interface Props {
  entry: ImportEntry;
}

/**
 * The reconciliation state of a row, already localised by the backend (`statusAsString`). A faulty row
 * (`hasError`) leads with an alert icon whose label carries the reason, so the message is reachable
 * without a column of its own.
 *
 * The reason is declared to the table's own tooltip (`data-tooltip`, the delegated
 * {@link useOverflowTooltip}) rather than as a native `title`: one styled tooltip for the whole grid,
 * announced by screen readers, and openable by tap on a phone — a native `title` is none of these.
 */
export function ImportStatusCell({ entry }: Props) {
  return (
    <span
      className={cn("flex items-center gap-1.5")}
      data-tooltip={entry.error ?? undefined}
    >
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
  );
}
