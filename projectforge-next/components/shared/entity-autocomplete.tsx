"use client";

import { useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  Cancel01Icon,
  SmileIcon,
  WinkIcon,
} from "@hugeicons/core-free-icons";
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
import { leafKeyOf } from "@/lib/leaf-key";
import { fetchAutoCompletion } from "@/lib/rs/dynamic";
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
  /**
   * Further request parameters the endpoint reads besides the search term — `{projektId}` narrows
   * `cost2/autosearch` to the cost units of one project (`Kost2PagesRest.queryAutocompleteObjects`).
   *
   * Part of the query key, so a changed value asks again instead of serving the previous answer.
   */
  params?: Record<string, unknown>;
  id?: string;
  /**
   * An entry the control offers with one click, beside the search — the logged-in user for a field that
   * asks for a person (see [useCurrentUserRef]). Hidden while it is already the value: there is nothing
   * to pick then. The legacy counterpart is the „select me" smiley of UserSelect.jsx.
   */
  selectMe?: T | null;
  /**
   * Whether picking an entry leaves the search open, with the cursor in its term. For a caller that
   * collects several entries (see [EntityMultiAutocompleteField]) one pick is not the end of the
   * interaction, and closing the popover would cost a click per member.
   */
  keepOpenOnSelect?: boolean;
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
  params,
  id,
  className,
  autoFocus,
  keepOpenOnSelect,
  selectMe,
  "aria-label": ariaLabel,
}: EntityAutocompleteProps<T>) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [winking, setWinking] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);

  const { data: found, isFetching } = useQuery({
    queryKey: ["entity-autocomplete", url, search, params ?? null],
    queryFn: ({ signal }) =>
      fetchAutoCompletion<T>(url, search, params, signal),
    // Below minChars the backend would answer with everything it has, which is neither useful nor
    // cheap — `user/autosearch` without a term lists every active user.
    enabled: open && search.trim().length >= minChars,
  });

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
        {selectMe && selectMe.id !== value?.id && (
          <button
            type="button"
            // As the reset button beside it: the button disappears the moment it is used, so a click
            // would end on a popover that no longer has anything under the pointer.
            onPointerDown={(e) => {
              e.preventDefault();
              onChange(selectMe);
            }}
            onPointerEnter={() => setWinking(true)}
            onPointerLeave={() => setWinking(false)}
            onFocus={() => setWinking(true)}
            onBlur={() => setWinking(false)}
            // The tooltip of the legacy smiley is a joke („You are great!"), which names nothing — the
            // accessible name says what the button does, the joke stays as the title.
            aria-label={`${t(leafKeyOf("select", t.has))}: ${selectMe.displayName}`}
            title={t("tooltip.selectMe")}
            className="shrink-0 cursor-pointer text-muted-foreground hover:text-foreground"
          >
            <HugeiconsIcon icon={winking ? WinkIcon : SmileIcon} size={16} />
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
            ref={searchRef}
            value={search}
            onValueChange={setSearch}
            placeholder={t("filter.search")}
          />
          <CommandList>
            <CommandEmpty>
              {search.trim().length < minChars
                ? t("filter.search")
                : isFetching
                  ? t("loading")
                  : t("nothingFound")}
            </CommandEmpty>
            {(found ?? []).map((entry) => (
              <CommandItem
                key={entry.id}
                value={String(entry.id)}
                onSelect={() => {
                  onChange(entry);
                  // The term goes either way: what was searched for has been found.
                  setSearch("");
                  if (!keepOpenOnSelect) {
                    setOpen(false);
                    return;
                  }
                  // Explicitly, and not by leaving focus alone: a pick by mouse leaves it on the
                  // item that was clicked, so the next term would go nowhere.
                  searchRef.current?.focus();
                }}
              >
                {entry.displayName}
              </CommandItem>
            ))}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
