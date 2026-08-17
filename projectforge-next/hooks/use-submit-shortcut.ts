"use client";

import { useCallback, type KeyboardEvent } from "react";
import { useTranslations } from "next-intl";
import { leafKeyOf } from "@/lib/leaf-key";
import { isSubmitShortcut } from "@/lib/submit-shortcut";

/**
 * Return (in a multi-line field `CTRL-Return`) triggers the default button of a form — what the
 * Wicket pages do with `Form.setDefaultButton`, for every form of this app.
 *
 * Returns the `onKeyDown` handler for the element that wraps the form: the `<form>` of an edit page,
 * the `DialogContent` of a dialog, the container of a server-laid-out page. Deliberately not a
 * `window` listener like the global shortcuts (`useAddEntryShortcut`): the shortcut belongs to the
 * form and not to the page, so a dialog open above an edit page must not save both.
 *
 * `preventDefault` on the way, which also takes the browser's own implicit submission out of the way
 * — otherwise a `<form>` with a single-line field would submit twice.
 *
 * @param onSubmit What the default button does.
 * @param enabled Same condition the default button is enabled under (dirty, not already saving). A
 *   shortcut that fires while the button is disabled would be a second, looser way in.
 */
export function useSubmitShortcut(
  onSubmit: () => void,
  enabled = true
): (event: KeyboardEvent) => void {
  return useCallback(
    (event: KeyboardEvent) => {
      if (!enabled || !isSubmitShortcut(event)) return;
      event.preventDefault();
      onSubmit();
    },
    [enabled, onSubmit]
  );
}

/**
 * The tooltip of the default button, naming the shortcut of [useSubmitShortcut] — written once, so
 * that every form explains it the same way.
 *
 * Split into the two props of [HintTooltip] rather than joined into one string: the heading is what
 * its `title` slot is for, and only there does it get a heading's weight and the space of a blank line
 * below it. Two markdown paragraphs in `text` would read as one block of prose.
 *
 * The key is a text *and* the parent of `.title`, so the catalog holds the text under `_` — resolved
 * through [leafKeyOf] instead of spelled out, because whether it collides is a property of the bundle.
 */
export function useSubmitShortcutHint(): { title: string; text: string } {
  const t = useTranslations();
  const key = "tooltip.shortcut.submitForm";
  return {
    title: t(`${key}.title`),
    text: t(leafKeyOf(key, t.has)),
  };
}
