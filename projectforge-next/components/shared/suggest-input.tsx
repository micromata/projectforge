"use client";

import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { Input } from "@/components/ui/input";
import {
  Popover,
  PopoverAnchor,
  PopoverContent,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";

/** Characters to type before asking the server, matching `AutoCompletion.minChars`. */
export const SUGGEST_MIN_CHARS = 2;

export interface SuggestInputProps {
  value: string;
  onChange: (value: string) => void;
  /**
   * The values the backend has already seen for what this box holds, for the term typed so far. A
   * function rather than a url, so a caller may narrow the lookup by another field of its form (a time
   * sheet's reference is looked up within its task) without this control knowing what a task is.
   */
  suggest: (search: string, signal?: AbortSignal) => Promise<string[]>;
  /**
   * What the suggestions are keyed by in the query cache, beside the search term — everything the
   * [suggest] closure reads besides its argument. Without it, a narrowed lookup would keep answering
   * with the previous context's hits.
   */
  queryKey: readonly unknown[];
  /**
   * Characters to type before the server is asked. Defaults to [SUGGEST_MIN_CHARS]; `0` asks straight
   * away, so focusing the empty box already offers what the backend has seen — a time sheet's location,
   * whose endpoint answers an empty search with all recent entries (see fetchLocationSuggestions).
   */
  minChars?: number;
  id?: string;
  invalid?: boolean;
  disabled?: boolean;
  required?: boolean;
  maxLength?: number;
  autoFocus?: boolean;
  placeholder?: string;
  onBlur?: () => void;
  "aria-label"?: string;
  className?: string;
}

/**
 * A free-text box that suggests values the backend has already seen for it — a time sheet's location, its
 * reference, the properties a `{category}/autocomplete?property=…` answers for
 * (`AbstractPagesRest.getAutoCompletionForProperty`).
 *
 * The suggestions are a convenience only: anything the user types is a valid value, which is what makes
 * this a different control from [EntityAutocomplete] — that one picks an *entity* and refuses everything
 * else, this one completes a string.
 *
 * Context-free on purpose, so both a hand-built form field ([StringSuggestField]) and the server
 * laid out one ([DynamicAutoCompleteInput]) render the same box.
 */
export function SuggestInput({
  value,
  onChange,
  suggest,
  queryKey,
  minChars = SUGGEST_MIN_CHARS,
  id,
  invalid,
  disabled,
  required,
  maxLength,
  autoFocus,
  placeholder,
  onBlur,
  "aria-label": ariaLabel,
  className,
}: SuggestInputProps) {
  const [open, setOpen] = useState(false);

  const { data: completions = [] } = useQuery({
    queryKey: ["suggest", ...queryKey, value],
    queryFn: ({ signal }) => suggest(value, signal),
    enabled: open && !disabled && value.length >= minChars,
    // While typing, keep the previous term's hits on screen until the next request answers. Without it
    // every keystroke changes the key, the data falls back to empty, and the list — shown only while
    // there are suggestions — empties and refills: a flicker. Not for the empty-field open though: there
    // is no term yet, so keeping a prior term's (differently sized) hits would only make the box appear
    // at one size and then jump to another. Empty open waits for its own answer and appears once.
    placeholderData: value.length > 0 ? keepPreviousData : undefined,
  });

  // What the box already holds is no suggestion — offering it would be a click that changes nothing.
  const suggestions = completions.filter((entry) => entry !== value);

  return (
    // `open` follows the focus state alone, never the async list: gating Radix's open on
    // `suggestions.length` made it flip as the query loaded, replaying the open animation. The list is
    // gated on the *content* instead — it mounts once when the hits arrive and updates in place after.
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverAnchor asChild>
        <Input
          id={id}
          value={value}
          disabled={disabled}
          required={required}
          maxLength={maxLength}
          autoFocus={autoFocus}
          placeholder={placeholder}
          aria-label={ariaLabel}
          // The suggestion list replaces the browser's own history dropdown.
          autoComplete="off"
          className={cn(invalid && "border-destructive", className)}
          onChange={(e) => {
            onChange(e.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          // A click when the box is already focused (so onFocus won't fire again) reopens the list —
          // clicking the field is how a user asks to see the recent entries again after dismissing it.
          onClick={() => setOpen(true)}
          // Leaving the field closes the list (tabbing away, clicking elsewhere). Picking a suggestion
          // does not blur the input — the buttons keep the focus with onMouseDown below — so the click
          // still lands before this would tear the list down.
          onBlur={() => {
            setOpen(false);
            onBlur?.();
          }}
        />
      </PopoverAnchor>
      {suggestions.length > 0 && (
        <PopoverContent
          align="start"
          className="w-(--radix-popover-trigger-width) p-1"
          // Keep the caret in the input while the list is open.
          onOpenAutoFocus={(e) => e.preventDefault()}
          // The caret lives in the anchor input, which is *outside* this content — so the moment the
          // list mounts Radix reads focus as "outside" and dismisses it. That is what made a Tab-focus
          // flash the box and lose it (a mouse focus had already settled before mount, so no move was
          // seen). Closing is our job here: the input's onBlur does it on a real leave.
          onFocusOutside={(e) => e.preventDefault()}
        >
          {suggestions.map((entry) => (
            <button
              key={entry}
              type="button"
              className="block w-full rounded px-2 py-1 text-left text-sm hover:bg-muted"
              // Keep the focus in the input so its onBlur doesn't fire and close the list before this
              // click is delivered — the box stays open on pick and closes on a real focus leave.
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => {
                onChange(entry);
                setOpen(false);
              }}
            >
              {entry}
            </button>
          ))}
        </PopoverContent>
      )}
    </Popover>
  );
}
