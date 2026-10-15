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
 * Puts the cursor into the field a form starts in, once, as soon as it is on screen.
 *
 * For adding an entry: the user came here to type, so the form opens where the typing begins —
 * the same courtesy the Wicket pages did with their focus-on-first-field. Not for editing an
 * existing entry, where focusing a filled field would only invite an accidental change.
 *
 * `field` names that field (a form's `autoFocus`, looked up by the `data-field` every [FieldShell]
 * writes out); without it the first text control of the form is taken. Naming one is what a form does
 * whose first field is not what the user came to fill in: an invoice opens in its subject, never in its
 * number — that number is the backend's and is only ever corrected (Wicket's `RechnungEditForm` set the
 * focus on the subject for the same reason).
 *
 * The effect deliberately has no dependency array: the fields exist only after the entity's preset
 * has loaded, several renders after the hook is first called, and the arguments don't change in
 * between. `focused` keeps it to a single focus rather than one per render.
 */
export function useFocusFirstField<T extends HTMLElement>(
  enabled: boolean,
  field?: string
): RefObject<T | null> {
  const ref = useRef<T | null>(null);
  const focused = useRef(false);

  useEffect(() => {
    if (!enabled || focused.current || !ref.current) return;
    // A named field that is not on the page (a section rendering its own body, a typo in the
    // declaration) falls back to the first one, so a form always opens somewhere sensible.
    const scope =
      (field &&
        ref.current.querySelector<HTMLElement>(`[data-field="${field}"]`)) ||
      ref.current;
    const control = Array.from(
      scope.querySelectorAll<HTMLElement>(TEXT_CONTROLS)
    ).find(
      (element) =>
        !element.hasAttribute("disabled") &&
        !element.hasAttribute("readonly") &&
        // A field inside a collapsed section has no box at all, so it cannot take the cursor.
        element.offsetParent !== null
    );
    if (!control) return;
    focused.current = true;
    control.focus();
  });

  return ref;
}
