"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, Cancel01Icon } from "@hugeicons/core-free-icons";
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
import { LookupLoadingRow } from "@/components/shared/lookup-loading-row";
import { isNearBottom, useEntityLookup } from "@/hooks/use-entity-lookup";
import { cn } from "@/lib/utils";

/** What `{entity}/autosearch` answers with (AbstractPagesRest.DisplayObject). */
export interface EntityRef {
  id: number;
  displayName: string;
}

export interface EntityAutocompleteProps<T extends EntityRef = EntityRef> {
  /** The lookup url from the layout, with its literal `:search` placeholder. */
  url: string;
  value: EntityRef | null;
  /**
   * Called with the picked entry **as the backend sent it**, not with a rebuilt `{id, displayName}`: a
   * search of its own may answer more than a `DisplayObject` does, and the caller decides what of it to
   * keep (see OrderPositionField, whose hits carry the order behind the position).
   */
  onChange: (value: T | null) => void;
  /** Characters before the lookup fires; the backend defaults it to 2. */
  minChars?: number;
  id?: string;
  /** Accessible name of the trigger, when no `<label htmlFor>` names it. */
  "aria-label"?: string;
  className?: string;
  autoFocus?: boolean;
}

/**
 * Picks one entity by searching the backend for it — a user, a project, a customer.
 *
 * Context-free on purpose: it takes a url and a value, so both the filter row and any hand-built
 * form can use it. The form fields of a server-laid-out page are served by DynamicSelect instead,
 * which does the same lookup but additionally handles multi-select, CREATABLE and writing through
 * DynamicLayoutProvider.
 */
export function EntityAutocomplete<T extends EntityRef = EntityRef>({
  url,
  value,
  onChange,
  minChars = 2,
  id,
  className,
  autoFocus,
  "aria-label": ariaLabel,
}: EntityAutocompleteProps<T>) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");

  // Opening shows the first entries without a term; typing narrows them, and scrolling the list asks
  // for more (see useEntityLookup).
  const {
    entries: found,
    isFetching,
    isLoadingMore,
    loadMore,
  } = useEntityLookup<T>({ url, search, open, minChars });

  return (
    <Popover
      open={open}
      onOpenChange={(next) => {
        setOpen(next);
        // Closing discards the term but never the value: Escape means "never mind", not "clear".
        if (!next) setSearch("");
      }}
    >
      {/* `min-w-0` on both, to keep the picker inside the width it was given: a flex item's automatic
          minimum size is its content, so the trigger would hold the width of the *whole* entity name
          however narrow its field is and push the reset button out of it — onto the field beside it,
          since a row of fields (a cost assignment) has nothing between the columns. The name is
          `truncate`d, but only within a box that was allowed to become narrower than it. */}
      <div className={cn("flex min-w-0 items-center gap-1", className)}>
        <PopoverTrigger asChild>
          <Button
            id={id}
            type="button"
            variant="outline"
            role="combobox"
            aria-expanded={open}
            aria-label={ariaLabel}
            autoFocus={autoFocus}
            className="h-8 min-w-0 flex-1 justify-between px-2 text-xs font-normal"
          >
            <span className={cn("truncate", !value && "text-muted-foreground")}>
              {value?.displayName ?? t("filter.chooseEntity")}
            </span>
            <HugeiconsIcon icon={ArrowDown01Icon} size={14} aria-hidden />
          </Button>
        </PopoverTrigger>
        {value && (
          <button
            type="button"
            // On pointer down, not click: the button unmounts as soon as the value is gone, and a
            // popover around it would take the missing pointerup for a click outside itself.
            onPointerDown={(e) => {
              e.preventDefault();
              onChange(null);
            }}
            aria-label={`${t("reset")}: ${ariaLabel ?? value.displayName}`}
            className="shrink-0 cursor-pointer text-muted-foreground hover:text-foreground"
          >
            <HugeiconsIcon icon={Cancel01Icon} size={12} />
          </button>
        )}
      </div>
      <PopoverContent
        align="start"
        className="w-(--radix-popover-trigger-width) min-w-56 p-0"
      >
        {/* The backend does the filtering; cmdk must not filter the results again. */}
        <Command shouldFilter={false}>
          <CommandInput
            value={search}
            onValueChange={setSearch}
            placeholder={t("filter.search")}
          />
          {/* cmdk's list is its own scroll container, so the next page is asked for from here. The
              arrow keys scroll it too, which pages for the keyboard as well. */}
          <CommandList
            onScroll={(event) => {
              if (isNearBottom(event.currentTarget)) loadMore();
            }}
          >
            <CommandEmpty>
              {/* Nothing typed yet and still empty means the lookup is either running or has nothing
                  to offer — the hint to type only fits a term that is too short to be looked up. */}
              {search.trim().length > 0 && search.trim().length < minChars
                ? t("filter.search")
                : isFetching
                  ? t("loading")
                  : t("nothingFound")}
            </CommandEmpty>
            {found.map((entry) => (
              <CommandItem
                key={entry.id}
                value={String(entry.id)}
                onSelect={() => {
                  onChange(entry);
                  setOpen(false);
                  setSearch("");
                }}
              >
                {entry.displayName}
              </CommandItem>
            ))}
            {isLoadingMore && <LookupLoadingRow />}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
