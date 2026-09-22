"use client";

import { useCallback, useEffect } from "react";
import { useRouter } from "next/navigation";
import { isTypingTarget } from "@/lib/typing-target";

/**
 * Whether a key event is the "new entry" chord — `N` or `+`, or `ALT-N` (macOS: `CTRL-ALT-N`).
 *
 * Not the HTML `accesskey` the legacy Wicket pages used (`WebConstants.ACCESS_KEY_ADD`): on macOS
 * the browsers demand `CTRL-ALT-N` for it and swallow it often enough that the shortcut was simply
 * unavailable there. A listener of our own works on every platform.
 *
 * `N` is matched by `event.code` rather than `event.key`, because `ALT-N` on macOS produces a dead
 * key (`˜`) instead of an `n` — `code` names the physical key and stays `KeyN`. `+` is matched by
 * `event.key`, because its physical key (and thus `code`) varies by keyboard layout while the
 * produced character does not.
 */
export function isAddEntryShortcut(event: KeyboardEvent): boolean {
  const isAddKey = event.code === "KeyN" || event.key === "+";
  if (!isAddKey || event.metaKey) return false;
  // A bare `n` is a character everywhere text is entered, so it may only act outside of one.
  // With ALT held it is no longer typing, hence the shortcut holds even inside a field.
  if (!event.altKey && (event.ctrlKey || isTypingTarget(event.target)))
    return false;
  return true;
}

/**
 * Runs `onTrigger` on the "new entry" chord (see `isAddEntryShortcut`). The building block shared by
 * the list pages and the calendar. `canTrigger` gets the last say before `preventDefault` — so a
 * caller that declines a keystroke leaves its default intact rather than swallowing it. `enabled`
 * lets a caller that has no target yet register nothing at all rather than a listener that does nothing.
 */
export function useAddEntryKeyboardShortcut(
  onTrigger: () => void,
  {
    enabled = true,
    canTrigger,
  }: { enabled?: boolean; canTrigger?: (event: KeyboardEvent) => boolean } = {}
) {
  useEffect(() => {
    if (!enabled) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (!isAddEntryShortcut(event)) return;
      if (canTrigger && !canTrigger(event)) return;
      event.preventDefault();
      onTrigger();
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onTrigger, enabled, canTrigger]);
}

/**
 * The "new entry" shortcut of every list page: it opens the entity's add page. Inert without an
 * `addHref` — the button carrying it (see AddEntryButton) may run an action instead, and owns that
 * shortcut itself then.
 */
export function useAddEntryShortcut(addHref?: string, isLegacy = false) {
  const router = useRouter();
  const navigate = useCallback(() => {
    if (!addHref) return;
    // The add page of a list whose form is still the legacy one belongs to another app and needs a
    // full page load, exactly as the button beside it does (see useEditTargets).
    if (isLegacy) window.location.href = addHref;
    else router.push(addHref);
  }, [addHref, isLegacy, router]);
  useAddEntryKeyboardShortcut(navigate, { enabled: Boolean(addHref) });
}
