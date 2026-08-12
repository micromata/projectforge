"use client";

import { useEffect, useRef, type RefObject } from "react";

/**
 * Controls that take typed text — the ones a form starts in.
 *
 * `[data-autofocus="skip"]` is how a control opts out although it is one: [DateInput] opens its
 * calendar on focus, so a date as the entry point of a form would greet the user with a popover over
 * the fields they came to fill in. A form of nothing but dates therefore starts in no field at all,
 * which is the quieter of the two wrong answers.
 */
const TEXT_CONTROLS = [
  'input:not([type="hidden"]):not([type="checkbox"]):not([type="radio"]):not([type="button"]):not([type="submit"]):not([data-autofocus="skip"])',
  'textarea:not([data-autofocus="skip"])',
].join(",");

/**
 * Puts the cursor into the first text field of a form, once, as soon as it is on screen.
 *
 * For adding an entry: the user came here to type, so the first field is where they want to be —
 * the same courtesy the Wicket pages did with their focus-on-first-field. Not for editing an
 * existing entry, where focusing a filled field would only invite an accidental change.
 *
 * The effect deliberately has no dependency array: the fields exist only after the entity's preset
 * has loaded, several renders after the hook is first called, and `enabled` doesn't change in
 * between. `focused` keeps it to a single focus rather than one per render.
 */
export function useFocusFirstField<T extends HTMLElement>(
  enabled: boolean
): RefObject<T | null> {
  const ref = useRef<T | null>(null);
  const focused = useRef(false);

  useEffect(() => {
    if (!enabled || focused.current || !ref.current) return;
    const field = Array.from(
      ref.current.querySelectorAll<HTMLElement>(TEXT_CONTROLS)
    ).find(
      (element) =>
        !element.hasAttribute("disabled") &&
        !element.hasAttribute("readonly") &&
        // A field inside a collapsed section has no box at all, so it cannot take the cursor.
        element.offsetParent !== null
    );
    if (!field) return;
    focused.current = true;
    field.focus();
  });

  return ref;
}
