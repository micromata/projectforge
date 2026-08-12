"use client";

import { useEffect, useRef, useState } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { Search01Icon } from "@hugeicons/core-free-icons";
import { Input } from "@/components/ui/input";
import { useDebouncedValue } from "@/hooks/use-debounced-value";

/** Long enough to swallow a word being typed, short enough not to feel like a delay. */
const SEARCH_DELAY_MS = 300;

export interface SearchInputProps {
  /** The search string as it is in effect, i.e. as the last [onChange] left it. */
  value: string;
  /** Called once the typing has settled — not per keystroke. */
  onChange: (value: string) => void;
  placeholder: string;
}

/**
 * The search box of a list: what the user types is shown at once, and reaches the caller 300 ms later.
 *
 * The search string goes to the server (`MagicFilter.searchString`), so a call per keystroke means a full
 * list query per keystroke — on the order book, seconds of server time and megabytes for a result nobody
 * sees. Debouncing here rather than in the query hook keeps the input responsive: the value it renders is
 * its own state, and only the settled one leaves.
 *
 * Owned by this component and not by the pages, so every list debounces the same way.
 */
export function SearchInput({
  value,
  onChange,
  placeholder,
}: SearchInputProps) {
  const [typed, setTyped] = useState(value);
  const debounced = useDebouncedValue(typed, SEARCH_DELAY_MS);

  // The caller's value wins whenever it changes for a reason other than this input: the filter was reset,
  // or a saved filter was applied. Compared against what was last sent, so the user's own typing — which
  // arrives back here as `value` a moment later — does not overwrite what has been typed meanwhile.
  const sent = useRef(value);
  useEffect(() => {
    if (value !== sent.current) {
      sent.current = value;
      setTyped(value);
    }
  }, [value]);

  useEffect(() => {
    if (debounced === sent.current) return;
    sent.current = debounced;
    onChange(debounced);
    // onChange is a fresh closure per render on most call sites, and depending on it would fire this on
    // every render of the page rather than on a settled value.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debounced]);

  return (
    <>
      <HugeiconsIcon
        icon={Search01Icon}
        size={14}
        className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground"
      />
      <Input
        value={typed}
        onChange={(e) => setTyped(e.target.value)}
        placeholder={placeholder}
        aria-label={placeholder}
        className="h-9 pl-9"
      />
    </>
  );
}
