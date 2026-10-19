"use client";

import { useEffect } from "react";

/**
 * Whether a key event is the cost search chord, `ALT-K` (macOS: `Option-K`). Matched by `event.code`,
 * as `Option-K` produces `˚` rather than a `k` (see isAddEntryShortcut). With ALT held it is no longer
 * typing, so it also works from inside a field — the user is usually in one when the number is needed.
 */
export function isCostSearchShortcut(event: KeyboardEvent): boolean {
  return (
    event.code === "KeyK" &&
    event.altKey &&
    !event.ctrlKey &&
    !event.metaKey &&
    !event.shiftKey
  );
}

/** Runs `onTrigger` on the cost search chord; registers nothing while not `enabled`. */
export function useCostSearchShortcut(onTrigger: () => void, enabled: boolean) {
  useEffect(() => {
    if (!enabled) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (!isCostSearchShortcut(event)) return;
      event.preventDefault();
      onTrigger();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onTrigger, enabled]);
}
