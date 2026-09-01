"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  Command,
  CommandEmpty,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { cn } from "@/lib/utils";

export interface ValueOption {
  value: string;
  label: string;
}

export interface ValueOptionListProps {
  options: ValueOption[];
  /** The picked options by value. */
  selected: string[];
  /** Accumulate the picks instead of replacing them. */
  multi?: boolean;
  onChange: (values: string[]) => void;
  /** A single pick is final; the combobox closes its popover on it. */
  onPicked?: () => void;
  autoFocus?: boolean;
  "aria-label"?: string;
  className?: string;
}

/**
 * A searchable list of options with the picked ones ticked.
 *
 * Its own component because it is needed both behind a trigger ([ValueCombobox], where a form grid
 * needs the field one line tall) and laid open in place ([ListField] in a filter pill, where a
 * second popover on top of the first would cover the pill's save button).
 */
export function ValueOptionList({
  options,
  selected,
  multi,
  onChange,
  onPicked,
  autoFocus,
  "aria-label": ariaLabel,
  className,
}: ValueOptionListProps) {
  const t = useTranslations("select");
  // Controlled only to highlight the match in the options; cmdk still does the filtering itself.
  const [search, setSearch] = useState("");

  return (
    <Command className={cn("bg-transparent", className)} label={ariaLabel}>
      <CommandInput
        placeholder={t("search")}
        autoFocus={autoFocus}
        value={search}
        onValueChange={setSearch}
      />
      <CommandList>
        <CommandEmpty>{t("noOptions")}</CommandEmpty>
        {options.map((option) => (
          <CommandItem
            key={option.value}
            value={option.label}
            // [CommandItem] brings its own trailing tick, shown on this flag.
            data-checked={selected.includes(option.value)}
            onSelect={() => toggle(option.value)}
          >
            <HighlightedText text={option.label} query={search} />
          </CommandItem>
        ))}
      </CommandList>
    </Command>
  );

  function toggle(value: string) {
    if (!multi) {
      onChange(selected.includes(value) ? [] : [value]);
      onPicked?.();
      return;
    }
    onChange(
      selected.includes(value)
        ? selected.filter((it) => it !== value)
        : [...selected, value]
    );
  }
}
