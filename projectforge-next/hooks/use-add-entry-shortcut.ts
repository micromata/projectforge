"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { isTypingTarget } from "@/lib/typing-target";

/**
 * The "new entry" shortcut of every list page: `N` or `+`, or `ALT-N` (macOS: `CTRL-ALT-N`).
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
export function useAddEntryShortcut(addHref: string, isLegacy = false) {
  const router = useRouter();

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const isAddKey = event.code === "KeyN" || event.key === "+";
      if (!isAddKey || event.metaKey) return;
      // A bare `n` is a character everywhere text is entered, so it may only act outside of one.
      // With ALT held it is no longer typing, hence the shortcut holds even inside a field.
      if (!event.altKey && (event.ctrlKey || isTypingTarget(event.target)))
        return;
      event.preventDefault();
      // The add page of a list whose form is still the legacy one belongs to another app and needs a
      // full page load, exactly as the button beside it does (see useEditTargets).
      if (isLegacy) window.location.href = addHref;
      else router.push(addHref);
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [addHref, isLegacy, router]);
}
