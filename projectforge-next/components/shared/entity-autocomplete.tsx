"use client";

import { type KeyboardEvent, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, Cancel01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import { EntitySearchList } from "./entity-search-list";
import { SelectMeButton } from "./select-me-button";

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
  /** Characters before a *typed* term is looked up; the backend defaults it to 2. */
  minChars?: number;
  /** Further request parameters of that search, see [EntitySearchList]. */
  params?: Record<string, unknown>;
  id?: string;
  /**
   * A value that must stay set: the reset (clear) button is left out, since clearing it to nothing is not
   * a valid state (e.g. the monthly report always reports on *some* user). The picker still lets another
   * entry replace the current one.
   */
  required?: boolean;
  /**
   * An entry the control offers with one click, beside the search — the logged-in user for a field that
   * asks for a person (see [useCurrentUserRef]). Hidden while it is already the value: there is nothing
   * to pick then.
   */
  selectMe?: T | null;
  /** Accessible name of the trigger, when no `<label htmlFor>` names it. */
  "aria-label"?: string;
  className?: string;
  autoFocus?: boolean;
  /**
   * Shown but not changeable — a value this user may read and not set, or a whole form that is only
   * being looked at (a deleted entry, see useFormReadOnly). The trigger cannot be opened and the two
   * buttons beside it are left out: both are ways of changing the value.
   */
  disabled?: boolean;
}

/**
 * Picks one entity by searching the backend for it — a user, a project, a customer.
 *
 * Context-free on purpose: it takes a url and a value, so both the filter row and any hand-built
 * form can use it. Several entities at once are [EntityMultiAutocomplete]'s business; the form fields of
 * a server-laid-out page are served by DynamicSelect instead, which does the same lookup but
 * additionally handles multi-select, CREATABLE and writing through DynamicLayoutProvider.
 */
export function EntityAutocomplete<T extends EntityRef = EntityRef>({
  url,
  value,
  onChange,
  minChars,
  params,
  id,
  className,
  autoFocus,
  selectMe,
  disabled,
  required,
  "aria-label": ariaLabel,
}: EntityAutocompleteProps<T>) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  // The term a keystroke on the (closed) trigger opens the picker with, so that first character is not
  // lost: the search input lives inside the popover and only exists once it is open, so without this a
  // user who tabs onto the trigger and starts typing would type into nothing until they clicked. Reset
  // whenever the popover closes, so a later open by click starts empty again.
  const [initialSearch, setInitialSearch] = useState("");

  function openOnTyping(event: KeyboardEvent<HTMLButtonElement>) {
    if (disabled) return;
    // Only printable single characters — leave Space (the button's own "open"), Enter, Tab, arrows and
    // any modifier combo (copy, browser shortcuts) alone.
    if (
      event.key.length !== 1 ||
      event.key === " " ||
      event.ctrlKey ||
      event.metaKey ||
      event.altKey
    ) {
      return;
    }
    event.preventDefault();
    setInitialSearch(event.key);
    setOpen(true);
  }

  return (
    <Popover
      open={open}
      onOpenChange={(next) => {
        if (!next) setInitialSearch("");
        setOpen(next);
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
            disabled={disabled}
            onKeyDown={openOnTyping}
            className="h-8 min-w-0 flex-1 justify-between px-2 text-xs font-normal"
          >
            <span className={cn("truncate", !value && "text-muted-foreground")}>
              {value?.displayName ?? t("filter.chooseEntity")}
            </span>
            <HugeiconsIcon icon={ArrowDown01Icon} size={14} aria-hidden />
          </Button>
        </PopoverTrigger>
        {value && !disabled && !required && (
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
        {selectMe && !disabled && selectMe.id !== value?.id && (
          <SelectMeButton me={selectMe} onPick={() => onChange(selectMe)} />
        )}
      </div>
      <PopoverContent
        align="start"
        className="w-(--radix-popover-trigger-width) min-w-56 p-0"
      >
        <EntitySearchList<T>
          url={url}
          params={params}
          minChars={minChars}
          active={open}
          initialSearch={initialSearch}
          onPick={(entry) => {
            onChange(entry);
            setOpen(false);
          }}
        />
      </PopoverContent>
    </Popover>
  );
}
