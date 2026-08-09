"use client";

import { useState } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import {
  Command,
  CommandEmpty,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
/** One pickable entry: a backend filter field, or a group of them like the change history. */
export interface FilterFieldPickerEntry {
  id: string;
  label: string;
  tooltip?: string;
}

interface FilterFieldPickerProps {
  entries: FilterFieldPickerEntry[];
  /** Fields already on the pill row; they get a checkmark and can't be picked again. */
  activeIds: string[];
  onSelect: (id: string) => void;
}

/**
 * Adds a filter field to the pill row.
 *
 * The list is long and mostly technical — the backend offers every search field of the
 * entity — so it needs a search, and matching on the raw id as well as the label is what
 * makes fields like "attachmentsIds" findable at all.
 *
 * Takes plain entries rather than [FilterElement]s, because not every entry is one field: the
 * change history is a single entry standing for three (see [historyFilterGroupOf]).
 */
export function FilterFieldPicker({
  entries,
  activeIds,
  onSelect,
}: FilterFieldPickerProps) {
  const t = useTranslations("filter");
  const [open, setOpen] = useState(false);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        {/* Icon-only: the pill row is where the eye goes, and a "+" chip reads as "add" there. */}
        <button
          type="button"
          title={t("addField")}
          aria-label={t("addField")}
          className="inline-flex cursor-pointer items-center rounded-full border border-dashed border-muted-foreground/40 px-2 py-1 text-muted-foreground hover:border-primary/40 hover:text-primary"
        >
          <HugeiconsIcon icon={PlusSignIcon} size={13} />
        </button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-72 p-0">
        <Command>
          <CommandInput placeholder={t("search")} aria-label={t("search")} />
          <CommandList>
            <CommandEmpty>{t("noMatch")}</CommandEmpty>
            {entries.map((entry) => (
              <CommandItem
                key={entry.id}
                value={`${entry.label} ${entry.id}`}
                title={entry.tooltip}
                data-checked={activeIds.includes(entry.id)}
                onSelect={() => {
                  setOpen(false);
                  onSelect(entry.id);
                }}
              >
                <span className="truncate">{entry.label}</span>
              </CommandItem>
            ))}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
