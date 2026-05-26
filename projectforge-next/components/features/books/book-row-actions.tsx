"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import {
  Copy01Icon,
  Delete01Icon,
  Edit02Icon,
} from "@hugeicons/core-free-icons";
import type { Book } from "./types";

export interface BookRowActionsProps {
  row: Book;
}

export function BookRowActions({ row }: BookRowActionsProps) {
  return (
    <>
      <button
        type="button"
        className="flex size-7 items-center justify-center rounded-sm bg-primary/10 text-primary hover:bg-primary/20"
        aria-label={`Buch ${row.titel} bearbeiten`}
      >
        <HugeiconsIcon icon={Edit02Icon} size={13} />
      </button>
      <button
        type="button"
        className="flex size-7 items-center justify-center rounded-sm bg-primary/10 text-primary hover:bg-primary/20"
        aria-label={`Buch ${row.titel} kopieren`}
      >
        <HugeiconsIcon icon={Copy01Icon} size={13} />
      </button>
      <button
        type="button"
        className="flex size-7 items-center justify-center rounded-sm hover:opacity-90"
        style={{
          background: "var(--status-loaned-bg)",
          color: "var(--status-loaned)",
        }}
        aria-label={`Buch ${row.titel} löschen`}
      >
        <HugeiconsIcon icon={Delete01Icon} size={13} />
      </button>
    </>
  );
}
