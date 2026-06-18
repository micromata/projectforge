"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  Copy01Icon,
  Delete01Icon,
  Edit02Icon,
} from "@hugeicons/core-free-icons";
import type { BookListRow } from "./types";

export interface BookRowActionsProps {
  row: BookListRow;
}

export function BookRowActions({ row }: BookRowActionsProps) {
  return (
    <>
      <button
        type="button"
        className="flex size-6 items-center justify-center rounded-sm bg-primary/10 text-primary hover:bg-primary/20"
        aria-label={`Buch ${row.title} bearbeiten`}
      >
        <HugeiconsIcon icon={Edit02Icon} size={12} />
      </button>
      <button
        type="button"
        className="flex size-6 items-center justify-center rounded-sm bg-primary/10 text-primary hover:bg-primary/20"
        aria-label={`Buch ${row.title} kopieren`}
      >
        <HugeiconsIcon icon={Copy01Icon} size={12} />
      </button>
      <button
        type="button"
        className="flex size-6 items-center justify-center rounded-sm hover:opacity-90"
        style={{
          background: "var(--status-loaned-bg)",
          color: "var(--status-loaned)",
        }}
        aria-label={`Buch ${row.title} löschen`}
      >
        <HugeiconsIcon icon={Delete01Icon} size={12} />
      </button>
    </>
  );
}
