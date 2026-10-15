"use client";

import { useEffect, useState } from "react";

/**
 * The value as it was `delay` ms ago, for the ones a request depends on.
 *
 * Cross-cutting rather than per feature: a search box that reaches the server on every keystroke
 * is the same problem everywhere — the list pages (see SearchInput) and the structure tree alike.
 */
export function useDebouncedValue<T>(value: T, delay = 250): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}
