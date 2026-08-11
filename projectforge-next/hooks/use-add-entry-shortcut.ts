"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

/**
 * The "new entry" shortcut of every list page: `N`, or `ALT-N` (macOS: `CTRL-ALT-N`).
 *
 * Not the HTML `accesskey` the legacy Wicket pages used (`WebConstants.ACCESS_KEY_ADD`): on macOS
 * the browsers demand `CTRL-ALT-N` for it and swallow it often enough that the shortcut was simply
 * unavailable there. A listener of our own works on every platform.
 *
 * `event.code` rather than `event.key`, because `ALT-N` on macOS produces a dead key (`˜`) instead
 * of an `n` — `code` names the physical key and stays `KeyN`.
 */
export function useAddEntryShortcut(addHref: string) {
  const router = useRouter();

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.code !== "KeyN" || event.metaKey) return;
      // A bare `n` is a character everywhere text is entered, so it may only act outside of one.
      // With ALT held it is no longer typing, hence the shortcut holds even inside a field.
      if (!event.altKey && (event.ctrlKey || isTyping(event.target))) return;
      event.preventDefault();
      router.push(addHref);
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [addHref, router]);
}

/**
 * Whether the key belongs to whoever is entering text — an input, a textarea, anything
 * `contenteditable`, or a widget that reads keys itself (a select, a combobox, a Radix menu).
 */
function isTyping(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  if (target.isContentEditable) return true;
  const tag = target.tagName;
  if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return true;
  return Boolean(
    target.closest('[contenteditable="true"],[role="combobox"],[role="menu"]')
  );
}
