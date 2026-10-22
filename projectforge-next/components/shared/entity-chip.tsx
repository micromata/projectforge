"use client";

import type { DragEvent } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import type { SelectModifiers } from "@/hooks/use-chip-selection";
import { inactiveRefClass } from "@/lib/page-def/reference";
import { cn } from "@/lib/utils";
import type { EntityRef } from "./entity-autocomplete";

export interface EntityChipProps {
  entry: EntityRef;
  selected: boolean;
  onSelect: (modifiers: SelectModifiers) => void;
  onRemove: () => void;
  /** Names the removing button („Löschen: Kai Reinhard"). */
  removeLabel: string;
  onDragStart: (event: DragEvent<HTMLSpanElement>) => void;
  onDragEnd: () => void;
}

/**
 * One picked entity of [EntityMultiAutocomplete]: its name and a button removing it.
 *
 * A chip can be selected by a click and dragged onto another picker of the same kind, as recipients
 * are in a mail client. It takes no focus of its own — the box around it does, so the keys (copy,
 * delete, Escape) are the box's, and Tab still walks the removing buttons.
 */
export function EntityChip({
  entry,
  selected,
  onSelect,
  onRemove,
  removeLabel,
  onDragStart,
  onDragEnd,
}: EntityChipProps) {
  return (
    <span
      role="option"
      aria-selected={selected}
      draggable
      onDragStart={onDragStart}
      onDragEnd={onDragEnd}
      onClick={(event) => onSelect(event)}
      className={cn(
        "inline-flex h-6 max-w-full cursor-default items-center gap-1 rounded-full border px-2 text-xs font-semibold select-none",
        selected
          ? "border-primary bg-primary text-primary-foreground"
          : "border-primary/25 bg-primary/10 text-primary"
      )}
    >
      <span className={cn("truncate", inactiveRefClass(entry))}>
        {entry.displayName}
      </span>
      <button
        type="button"
        onClick={(event) => {
          // Removing is no click on the chip: it would select the chip it just took away.
          event.stopPropagation();
          onRemove();
        }}
        aria-label={removeLabel}
        className="cursor-pointer opacity-60 hover:opacity-100"
      >
        <HugeiconsIcon icon={Cancel01Icon} size={12} />
      </button>
    </span>
  );
}
