"use client";

import { CommandGroup, CommandItem } from "@/components/ui/command";
import type { EntityRef } from "./entity-autocomplete";
import { freeTextOffer } from "./free-text-offer";

/** What a search list needs to also offer the typed term itself, see [EntitySearchListProps.freeText]. */
export interface FreeTextOption {
  /** Heading of the group, which names what is offered: not a record, but the term as text. */
  heading: string;
  /** Text of the one entry, built from the term (`Use as free text: “ACME”`). */
  itemLabel: (term: string) => string;
  /** The longest text the field takes; a longer term is offered but cannot be picked. */
  maxLength?: number;
  /** Explains why a too long term cannot be picked, see [maxLength]. */
  tooLongLabel?: (maxLength: number) => string;
  onPick: (term: string) => void;
}

/**
 * The term itself as a choice — the last group of the list, under the records found for it.
 *
 * Last on purpose: cmdk highlights the first entry, so Enter takes a matching record, and a free text
 * has to be chosen deliberately. Left out when a record is named exactly like the term (that record is
 * what was meant) and while nothing is typed.
 */
export function EntitySearchFreeText({
  term,
  entries,
  option,
}: {
  term: string;
  entries: EntityRef[];
  option: FreeTextOption;
}) {
  const offer = freeTextOffer(
    term,
    entries.map((entry) => entry.displayName),
    option.maxLength
  );
  if (!offer) return null;
  const { text, tooLong } = offer;
  return (
    <CommandGroup heading={option.heading}>
      <CommandItem
        // Own value prefix, so the entry never collides with a record's id (cmdk keys items by value).
        value={`free-text-${text}`}
        disabled={tooLong}
        onSelect={() => option.onPick(text)}
        className="italic"
      >
        {tooLong && option.tooLongLabel
          ? option.tooLongLabel(option.maxLength!)
          : option.itemLabel(text)}
      </CommandItem>
    </CommandGroup>
  );
}
