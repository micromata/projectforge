/**
 * Whether a key event happened where Return is a character of its own: a textarea, or anything
 * `contenteditable`.
 *
 * The line break is what Return means there, so the shortcut needs a modifier — the rule of the
 * Wicket pages (`projectforge.js`) and of the legacy React client (`TextArea.jsx`) alike.
 */
export function isMultilineTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  return target.tagName === "TEXTAREA" || target.isContentEditable;
}

/**
 * Whether Return in this target means "pick the highlighted entry" instead of "submit": a combobox, a
 * listbox, a menu.
 *
 * `defaultPrevented` catches most of them; this is the belt to that pair of braces, since a widget may
 * act on `keyup` or leave the event alone.
 */
function isChoosingTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  return Boolean(
    target.closest('[role="combobox"],[role="listbox"],[role="menu"]')
  );
}

/** The part of a keyboard event the rule reads — React's synthetic one and the native one alike. */
export interface SubmitShortcutKeys {
  key: string;
  ctrlKey: boolean;
  metaKey: boolean;
  altKey: boolean;
  shiftKey: boolean;
  defaultPrevented: boolean;
  /** A Return that finishes an IME composition belongs to the composition, not to the form. */
  isComposing?: boolean;
  /** React puts `isComposing` on the native event only. */
  nativeEvent?: { isComposing?: boolean };
}

/**
 * The rule itself, over the keys alone: Return submits, and in a multi-line field `CTRL-Return`
 * (macOS: `CMD-Return`).
 *
 * The same rule everywhere, so that a form behaves alike whether the browser would submit it by
 * itself (an `<input>` inside a `<form>`) or not (a textarea, a dialog, a server-laid-out page).
 * `CTRL-Return` works in a single-line field too: the user reaching for the two keys always means the
 * same thing, and having to remember where the modifier is needed would be the worse rule.
 *
 * What it deliberately leaves alone:
 * - **`defaultPrevented`** — the most important filter. [DateInput], [TimeInput], [TagInput] and
 *   [NumberSegmentInput] read Return themselves and call `preventDefault`, as do the Radix widgets
 *   that pick an option with it. Whoever already gave Return a meaning keeps it.
 * - **`ALT`/`SHIFT`** — `SHIFT-Return` is the line break of a textarea, and `ALT` is a shortcut
 *   prefix of its own on macOS.
 *
 * @param multiline Whether Return is a character where the event happened (see [isMultilineTarget]).
 */
export function isSubmitKey(
  event: SubmitShortcutKeys,
  multiline: boolean
): boolean {
  if (event.key !== "Enter") return false;
  if (event.defaultPrevented) return false;
  if (event.altKey || event.shiftKey) return false;
  if (event.isComposing || event.nativeEvent?.isComposing) return false;
  // One or the other, never both — the idiom of useSelectAllShortcut, so that holding CTRL *and* CMD
  // is not a second way in.
  const withModifier = event.ctrlKey !== event.metaKey;
  return withModifier || !multiline;
}

/** [isSubmitKey] for a real event, reading the multi-line and the choosing case off its target. */
export function isSubmitShortcut(
  event: SubmitShortcutKeys & { target: EventTarget | null }
): boolean {
  if (!isSubmitKey(event, isMultilineTarget(event.target))) return false;
  return !isChoosingTarget(event.target);
}
