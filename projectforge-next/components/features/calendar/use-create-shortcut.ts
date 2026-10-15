"use client";

import { useCallback } from "react";
import { useAddEntryKeyboardShortcut } from "@/hooks/use-add-entry-shortcut";

/**
 * The calendar's counterpart to the list pages' "new entry" shortcut (`N` / `+` / `ALT-N`, see
 * useAddEntryShortcut): the same chord opens a new entry here too. Unlike a list there is no fixed
 * add url — the backend decides timesheet vs. team event from the filter — so it runs `onCreate`
 * (the "+" toolbar button's handler) rather than navigating.
 *
 * Suppressed while an edit already overlays the calendar: the modal stays mounted over the calendar
 * (see CalendarShell), and while its own inputs are guarded by isTypingTarget, a keystroke on a
 * non-typing element inside it must not stack a second new entry behind the open dialog.
 */
export function useCreateShortcut(onCreate: () => void) {
  const notWhileEditing = useCallback(
    () => !document.querySelector('[role="dialog"][data-state="open"]'),
    []
  );
  useAddEntryKeyboardShortcut(onCreate, { canTrigger: notWhileEditing });
}
