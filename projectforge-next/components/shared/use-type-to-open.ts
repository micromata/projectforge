"use client";

import { type KeyboardEvent, useState } from "react";

/**
 * The open state of a picker whose search input lives inside its popover — and the keystroke that opens
 * it from the closed trigger, shared by [EntityAutocomplete] and [EntityOrTextAutocomplete].
 *
 * The term a keystroke on the (closed) trigger opens the picker with is kept, so that first character is
 * not lost: the search input only exists once the popover is open, so without this a user who tabs onto
 * the trigger and starts typing would type into nothing until they clicked. Reset whenever the popover
 * closes, so a later open by click starts empty again.
 */
export function useTypeToOpen({
  initiallyOpen,
  disabled,
}: {
  /** Read once, on mount — a later change must not reopen the picker, that is the user's to do. */
  initiallyOpen: boolean;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(initiallyOpen);
  const [initialSearch, setInitialSearch] = useState("");

  function openOnTyping(event: KeyboardEvent<HTMLButtonElement>) {
    if (disabled) return;
    // Only printable single characters — leave Space (the button's own "open"), Enter, Tab, arrows and
    // any modifier combo (copy, browser shortcuts) alone.
    if (
      event.key.length !== 1 ||
      event.key === " " ||
      event.ctrlKey ||
      event.metaKey ||
      event.altKey
    ) {
      return;
    }
    event.preventDefault();
    setInitialSearch(event.key);
    setOpen(true);
  }

  function onOpenChange(next: boolean) {
    // The trigger of a disabled picker is only aria-disabled, so its value stays selectable (see
    // useCopyableTrigger) — which leaves it to this to keep the popover shut.
    if (next && disabled) return;
    if (!next) setInitialSearch("");
    setOpen(next);
  }

  return { open, onOpenChange, initialSearch, openOnTyping };
}
