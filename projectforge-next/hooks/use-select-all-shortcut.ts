"use client";

import { useEffect } from "react";
import { isTypingTarget } from "@/lib/typing-target";

/**
 * `CTRL-A` (macOS: `CMD-A`) ticks every entry of the list, while it is in selection mode.
 *
 * Takes over the browser's "select all text", which inside a table of picked rows is not what the
 * shortcut means any more — and only there: outside the mode, and inside any input, it is left alone
 * (`isTypingTarget`, as the add-entry shortcut does it).
 *
 * "Every entry" is the whole result set and not the page on screen: the list holds all of it on the
 * client and pages it here, which is also the set the backend registered for selection.
 */
export function useSelectAllShortcut(
  active: boolean,
  selectAll: () => void
): void {
  useEffect(() => {
    if (!active) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.code !== "KeyA") return;
      // One or the other, never both — and never ALT, which is a shortcut of its own on macOS.
      if (event.ctrlKey === event.metaKey || event.altKey) return;
      if (isTypingTarget(event.target)) return;
      event.preventDefault();
      selectAll();
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [active, selectAll]);
}
