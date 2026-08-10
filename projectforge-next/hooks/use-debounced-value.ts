"use client";

import { useEffect, useState } from "react";

/**
 * The value as it was `delay` ms ago, for the ones a request depends on.
 *
 * Cross-cutting rather than per feature: a search box that reaches the server on every keystroke
 * is the same problem everywhere. The list pages don't need it — their filter is applied on the
 * client — but a server-filtered one (the structure tree) does.
 */
export function useDebouncedValue<T>(value: T, delay = 250): T {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}
