"use client";

import { useCallback, useEffect, useRef, useState } from "react";

/** How long a copy button shows its tick before it is a copy button again. */
export const COPIED_RESET_MS = 2000;

/**
 * Puts a value on the clipboard and says so for a moment: `copied` is true for [COPIED_RESET_MS]
 * after a copy, then false again — so the button that shows the tick offers to copy once more,
 * rather than looking done for good. A second copy within that time starts the moment over.
 */
export function useCopyToClipboard(): {
  copied: boolean;
  copy: (value: string | null | undefined) => Promise<void>;
} {
  const [copied, setCopied] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(
    () => () => {
      if (timer.current) clearTimeout(timer.current);
    },
    []
  );

  const copy = useCallback(async (value: string | null | undefined) => {
    if (!value) return;
    await navigator.clipboard.writeText(value);
    setCopied(true);
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => setCopied(false), COPIED_RESET_MS);
  }, []);

  return { copied, copy };
}
