"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverAnchor,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useFormatContext } from "@/hooks/use-format";
import { compareText } from "@/lib/format";
import { refTypeOfUrl } from "@/lib/entity-ref-transfer";
import { cn } from "@/lib/utils";
import type { EntityRef } from "./entity-autocomplete";
import type { InactiveMode } from "@/hooks/use-show-inactive";
import { EntityChip } from "./entity-chip";
import { GroupMembersHint } from "./group-members-hint";
import { EntitySearchList } from "./entity-search-list";
import { useChipTransfer } from "./use-chip-transfer";
import { SelectMeButton } from "./select-me-button";

export interface EntityMultiAutocompleteProps {
  /** The lookup url, with its literal `:search` placeholder — as [EntityAutocomplete] takes it. */
  url: string;
  value: EntityRef[];
  onChange: (value: EntityRef[]) => void;
  /** Characters before the lookup fires; the backend defaults it to 2. */
  minChars?: number;
  /** Further request parameters of that search, see [EntitySearchList]. */
  params?: Record<string, unknown>;
  /** How inactive entries are offered, see [InactiveMode]. */
  inactive?: InactiveMode;
  id?: string;
  /** Accessible name of the trigger, when no `<label htmlFor>` names it. */
  "aria-label"?: string;
  className?: string;
  /** Names the removing buttons of the chips („Löschen: Kai Reinhard"). */
  removeLabel: (entry: EntityRef) => string;
  /**
   * Shows the chips alphabetically, whatever order they were picked in — for a set of members, where
   * the order means nothing. The value itself keeps its order, so sorting marks no form dirty.
   */
  sorted?: boolean;
  /**
   * The logged-in user, offered as a one-click pick beside the field — for a field that asks for people
   * (see [useCurrentUserRef]). Hidden while the user is picked already: there is nothing to add then.
   */
  selectMe?: EntityRef | null;
  /** Opens the search at once while nothing is picked — for a dialog that is about picking (see [EntityAutocomplete]). */
  autoOpen?: boolean;
  /**
   * The kind of entity picked, for moving chips to another picker of the same kind (see [useChipTransfer]).
   * Taken from the url (`user/autosearch` → `user`); given only where the url names something else.
   */
  refType?: string;
}

/**
 * Picks any number of entities by searching the backend for them — the members of a group.
 *
 * What is picked stands **inside** the control, as removable chips, the way the legacy multi select shows
 * it (`react-select` with `isMulti`): the search stays open while one adds a whole team, and a list below
 * the control would be exactly what the open popover covers.
 *
 * Chips can be selected and dragged, or copied and pasted, into another picker of the same kind
 * ([useChipTransfer]) — the members of one group into the access list of a calendar.
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
  inactive,
  id,
  className,
  removeLabel,
  sorted,
  selectMe,
  autoOpen,
  refType,
  "aria-label": ariaLabel,
}: EntityMultiAutocompleteProps) {
  const t = useTranslations();
  const format = useFormatContext();
  const [open, setOpen] = useState(!!autoOpen && value.length === 0);
  const chips = sorted
    ? [...value].sort((a, b) =>
        compareText(a.displayName, b.displayName, format)
      )
    : value;
  const chipType = refType ?? refTypeOfUrl(url);
  const { boxProps, chipProps, dropTarget, change } = useChipTransfer({
    refType: chipType,
    value,
    chips,
    onChange,
  });

  return (
    <Popover open={open} onOpenChange={setOpen}>
      {/* The anchor is the whole box, not the trigger inside it: the popover follows the field's left
          edge and takes its width, which the trigger has none of once the chips push it into a corner. */}
      {/* The select-me button stands beside the box, as it does beside the single picker. */}
      <div className={cn("flex min-w-0 items-center gap-1", className)}>
        <PopoverAnchor asChild>
          <div
            {...boxProps}
            aria-label={ariaLabel}
            className={cn(
              // The look of a text field (see components/ui/input.tsx), because that is what it is: a
              // control one puts values into, only that the values are chips.
              "flex min-h-7 min-w-0 flex-1 flex-wrap items-center gap-1.5 rounded-md border border-input bg-input/20 px-2 py-px outline-none focus-within:border-ring focus-within:ring-2 focus-within:ring-ring/30 dark:bg-input/30",
              // Chips of the same kind dragged over it: it takes them.
              dropTarget && "border-ring ring-2 ring-ring/30"
            )}
          >
            {chips.map((entry) => (
              <EntityChip
                key={entry.id}
                {...chipProps(entry, removeLabel(entry))}
                // Who a group stands for is what one wants to know before granting it anything.
                hint={
                  chipType === "group" ? (
                    <GroupMembersHint groupId={entry.id} />
                  ) : undefined
                }
              />
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
                // (`aria-expanded:bg-muted`). As narrow as its caret: a wider minimum would push it onto a line of
                // its own beside chips that still leave room for it, making the box two lines for nothing.
                className="h-6 min-w-6 flex-1 justify-between gap-1 px-0 text-xs font-normal text-muted-foreground hover:bg-transparent aria-expanded:bg-transparent dark:hover:bg-transparent"
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
        {selectMe && !value.some((picked) => picked.id === selectMe.id) && (
          <SelectMeButton
            me={selectMe}
            onPick={() => change([...value, selectMe])}
          />
        )}
      </div>
      <PopoverContent
        align="start"
        // As wide as the field, but capped: a members field spans the whole form, and a search list
        // the width of the page would be a lot of white beside very short names.
        className="w-(--radix-popover-trigger-width) max-w-md min-w-56 p-0"
      >
        <EntitySearchList
          url={url}
          params={params}
          inactive={inactive}
          minChars={minChars}
          active={open}
          // Adding is a series, not a single act: the search stays open with its term and the cursor
          // in it, so the next match is clicked and not searched for again.
          collecting
          excludeIds={new Set(value.map((picked) => picked.id))}
          onPick={(entry) => {
            // Silently ignored rather than reported: the same entry twice is no error, the search
            // simply answers what is already there — and its chip says so.
            if (value.some((picked) => picked.id === entry.id)) return;
            change([...value, entry]);
          }}
        />
      </PopoverContent>
    </Popover>
  );
}
