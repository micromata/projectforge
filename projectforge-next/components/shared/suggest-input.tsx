"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
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
    enabled: open && !disabled && value.length >= SUGGEST_MIN_CHARS,
  });

  // What the box already holds is no suggestion — offering it would be a click that changes nothing.
  const suggestions = completions.filter((entry) => entry !== value);

  return (
    <Popover open={open && suggestions.length > 0} onOpenChange={setOpen}>
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
          onBlur={onBlur}
        />
      </PopoverAnchor>
      <PopoverContent
        align="start"
        className="w-(--radix-popover-trigger-width) p-1"
        // Keep the caret in the input while the list is open.
        onOpenAutoFocus={(e) => e.preventDefault()}
      >
        {suggestions.map((entry) => (
          <button
            key={entry}
            type="button"
            className="block w-full rounded px-2 py-1 text-left text-sm hover:bg-muted"
            onClick={() => {
              onChange(entry);
              setOpen(false);
            }}
          >
            {entry}
          </button>
        ))}
      </PopoverContent>
    </Popover>
  );
}
