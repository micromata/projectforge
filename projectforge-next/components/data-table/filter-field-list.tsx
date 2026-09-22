"use client";

import { HugeiconsIcon } from "@hugeicons/react";
import { FilterIcon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import {
  Command,
  CommandEmpty,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { HintTooltip } from "@/components/shared/hint-tooltip";

/** One pickable entry: a backend filter field, or a group of them like the change history. */
export interface FilterFieldPickerEntry {
  id: string;
  label: string;
  tooltip?: string;
}

interface FilterFieldListProps {
  entries: FilterFieldPickerEntry[];
  /** Fields already on the pill row; they get a checkmark. */
  activeIds: string[];
  onSelect: (id: string) => void;
  /** Switches to the panel with every field at once. */
  onShowAll: () => void;
}

/**
 * The searchable list of filter fields.
 *
 * The list is long and mostly technical — the backend offers every search field of the entity —
 * so it needs a search, and matching on the raw id as well as the label is what makes fields like
 * "attachmentsIds" findable at all.
 *
 * Takes plain entries rather than [FilterElement]s, because not every entry is one field: the
 * change history is a single entry standing for three (see [historyFilterGroupOf]).
 */
export function FilterFieldList({
  entries,
  activeIds,
  onSelect,
  onShowAll,
}: FilterFieldListProps) {
  const t = useTranslations("filter");

  return (
    <Command>
      <CommandInput placeholder={t("search")} aria-label={t("search")} />
      {/* Outside the CommandList on purpose: as an item, typing in the search would filter it away. */}
      <button
        type="button"
        onClick={onShowAll}
        className="flex w-full cursor-pointer items-center gap-2 border-b px-3 py-2 text-sm text-muted-foreground hover:bg-accent hover:text-accent-foreground"
      >
        <HugeiconsIcon icon={FilterIcon} size={13} />
        {t("allFilters")}
      </button>
      <CommandList>
        <CommandEmpty>{t("noMatch")}</CommandEmpty>
        {entries.map((entry) => (
          // The tooltip sits on the right: an item's own explanation would cover the items below it.
          <HintTooltip key={entry.id} text={entry.tooltip} side="right">
            <CommandItem
              value={`${entry.label} ${entry.id}`}
              data-checked={activeIds.includes(entry.id)}
              onSelect={() => onSelect(entry.id)}
            >
              <span className="truncate">{entry.label}</span>
            </CommandItem>
          </HintTooltip>
        ))}
      </CommandList>
    </Command>
  );
}
