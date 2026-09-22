/**
 * Whether a key event belongs to whoever is entering text — an input, a textarea, anything
 * `contenteditable`, or a widget that reads keys itself (a select, a combobox, a Radix menu).
 *
 * What a global shortcut has to ask before it claims a bare key: `N` is a character wherever text
 * is entered. A shortcut held with a modifier is no longer typing and does not need this.
 */
export function isTypingTarget(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLElement)) return false;
  if (target.isContentEditable) return true;
  const tag = target.tagName;
  if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return true;
  return Boolean(
    target.closest('[contenteditable="true"],[role="combobox"],[role="menu"]')
  );
}
