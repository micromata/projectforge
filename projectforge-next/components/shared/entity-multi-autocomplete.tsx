"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, Cancel01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverAnchor,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import type { EntityRef } from "./entity-autocomplete";
import { EntitySearchList } from "./entity-search-list";

export interface EntityMultiAutocompleteProps {
  /** The lookup url, with its literal `:search` placeholder — as [EntityAutocomplete] takes it. */
  url: string;
  value: EntityRef[];
  onChange: (value: EntityRef[]) => void;
  /** Characters before the lookup fires; the backend defaults it to 2. */
  minChars?: number;
  /** Further request parameters of that search, see [EntitySearchList]. */
  params?: Record<string, unknown>;
  id?: string;
  /** Accessible name of the trigger, when no `<label htmlFor>` names it. */
  "aria-label"?: string;
  className?: string;
  /** Names the removing buttons of the chips („Löschen: Kai Reinhard"). */
  removeLabel: (entry: EntityRef) => string;
}

/**
 * Picks any number of entities by searching the backend for them — the members of a group.
 *
 * What is picked stands **inside** the control, as removable chips, the way the legacy multi select shows
 * it (`react-select` with `isMulti`): the search stays open while one adds a whole team, and a list below
 * the control would be exactly what the open popover covers.
 *
 * The chips are siblings of the trigger, not its content: a `<button>` inside a `<button>` is invalid
 * HTML, and the popover would swallow the clicks on the removing buttons.
 */
export function EntityMultiAutocomplete({
  url,
  value,
  onChange,
  minChars,
  params,
  id,
  className,
  removeLabel,
  "aria-label": ariaLabel,
}: EntityMultiAutocompleteProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      {/* The anchor is the whole box, not the trigger inside it: the popover follows the field's left
          edge and takes its width, which the trigger has none of once the chips push it into a corner. */}
      <PopoverAnchor asChild>
        <div
          className={cn(
            // The look of a text field (see components/ui/input.tsx), because that is what it is: a
            // control one puts values into, only that the values are chips.
            "flex min-h-8 min-w-0 flex-wrap items-center gap-1.5 rounded-md border border-input bg-input/20 px-2 py-1 focus-within:border-ring focus-within:ring-2 focus-within:ring-ring/30 dark:bg-input/30",
            className
          )}
        >
          {value.map((entry) => (
            <span
              key={entry.id}
              className="inline-flex h-6 max-w-full items-center gap-1 rounded-full border border-primary/25 bg-primary/10 px-2 text-xs font-semibold text-primary"
            >
              <span className="truncate">{entry.displayName}</span>
              <button
                type="button"
                onClick={() => onChange(value.filter((e) => e.id !== entry.id))}
                aria-label={removeLabel(entry)}
                className="cursor-pointer opacity-60 hover:opacity-100"
              >
                <HugeiconsIcon icon={Cancel01Icon} size={12} />
              </button>
            </span>
          ))}
          <PopoverTrigger asChild>
            {/* The box carries the border, so the trigger inside it has none of its own — it is the
                empty rest of the row, and clicking there opens the search. */}
            <Button
              id={id}
              type="button"
              variant="ghost"
              role="combobox"
              aria-expanded={open}
              aria-label={ariaLabel}
              // No background of its own in any state — the box behind it is the field, and the
              // ghost variant would paint a second one over it while the search is open
              // (`aria-expanded:bg-muted`).
              className="h-6 min-w-24 flex-1 justify-between gap-1 px-0 text-xs font-normal text-muted-foreground hover:bg-transparent aria-expanded:bg-transparent dark:hover:bg-transparent"
            >
              {/* Only while nothing is picked: with chips in the box the invitation is said twice. */}
              <span className="truncate">
                {value.length === 0 ? t("filter.chooseEntity") : ""}
              </span>
              <HugeiconsIcon icon={ArrowDown01Icon} size={14} aria-hidden />
            </Button>
          </PopoverTrigger>
        </div>
      </PopoverAnchor>
      <PopoverContent
        align="start"
        // As wide as the field, but capped: a members field spans the whole form, and a search list
        // the width of the page would be a lot of white beside very short names.
        className="w-(--radix-popover-trigger-width) max-w-md min-w-56 p-0"
      >
        <EntitySearchList
          url={url}
          params={params}
          minChars={minChars}
          active={open}
          // Adding is a series, not a single act: the search stays open with the cursor in its term,
          // so the next name is typed and not clicked open again.
          keepFocus
          onPick={(entry) => {
            // Silently ignored rather than reported: the same entry twice is no error, the search
            // simply answers what is already there — and its chip says so.
            if (value.some((picked) => picked.id === entry.id)) return;
            onChange([...value, entry]);
          }}
        />
      </PopoverContent>
    </Popover>
  );
}
