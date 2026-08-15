"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, Cancel01Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
import { cn } from "@/lib/utils";

export interface ValueOption {
  value: string;
  label: string;
}

export interface ValueComboboxProps {
  id?: string;
  options: ValueOption[];
  /** The picked options by value; an unknown value is shown as itself. */
  selected: string[];
  /** Accumulate the picks instead of replacing them, and keep the popover open. */
  multi?: boolean;
  onChange: (values: string[]) => void;
  /** Shown while nothing is picked. */
  placeholder?: string;
  "aria-label"?: string;
  className?: string;
}

/**
 * A fixed list of options as a one-line combobox: the picks sit in the trigger as removable
 * badges, the options live in a searchable popover.
 *
 * One line tall whatever the option count, which is what a form grid needs — a flat checkbox
 * list is as tall as its longest field and breaks the row rhythm around it.
 *
 * [DynamicSelect] renders the same shape but cannot use this: it also has to offer a server
 * lookup and a value the user typed, and its state lives in the layout context.
 */
export function ValueCombobox({
  id,
  options,
  selected,
  multi,
  onChange,
  placeholder,
  "aria-label": ariaLabel,
  className,
}: ValueComboboxProps) {
  const t = useTranslations("select");
  const [open, setOpen] = useState(false);

  function toggle(value: string) {
    if (!multi) {
      onChange(selected.includes(value) ? [] : [value]);
      setOpen(false);
      return;
    }
    onChange(
      selected.includes(value)
        ? selected.filter((it) => it !== value)
        : [...selected, value]
    );
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          id={id}
          variant="outline"
          role="combobox"
          aria-label={ariaLabel}
          className={cn(
            "h-auto min-h-8 w-full justify-between gap-1 font-normal text-xs",
            className
          )}
        >
          <span className="flex flex-wrap items-center gap-1">
            {selected.length === 0 && (
              <span className="text-muted-foreground">
                {placeholder ?? t("empty")}
              </span>
            )}
            {selected.map((value) => {
              const label = labelOf(options, value);
              return multi ? (
                <Badge key={value} variant="secondary">
                  {label}
                  <span
                    role="button"
                    tabIndex={-1}
                    aria-label={t("remove", { label })}
                    onClick={(e) => {
                      e.stopPropagation();
                      onChange(selected.filter((it) => it !== value));
                    }}
                  >
                    <HugeiconsIcon icon={Cancel01Icon} />
                  </span>
                </Badge>
              ) : (
                <span key={value}>{label}</span>
              );
            })}
          </span>
          <HugeiconsIcon icon={ArrowDown01Icon} size={14} />
        </Button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        className="w-(--radix-popover-trigger-width) min-w-56 p-0"
      >
        <Command>
          <CommandInput placeholder={t("search")} />
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
                {option.label}
              </CommandItem>
            ))}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}

/** A value with no option of its own is shown as it is, rather than vanishing from the trigger. */
function labelOf(options: ValueOption[], value: string): string {
  return options.find((it) => it.value === value)?.label ?? value;
}
