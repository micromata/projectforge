/**
 * The pure part of [Checklist]: which entries a search shows and what "select all" does to them.
 *
 * As in Excel's autofilter, "select all" acts on what the search shows — ticking it after typing "gmbh"
 * adds the matching entries to those already picked, unticking it removes only them.
 */

/** True if every word of [term] occurs in [label], case-insensitively; an empty term matches all. */
export function matchesTerm(label: string, term: string): boolean {
  const words = term.trim().toLocaleLowerCase().split(/\s+/).filter(Boolean);
  if (words.length === 0) return true;
  const text = label.toLocaleLowerCase();
  return words.every((word) => text.includes(word));
}

/** The state of the "select all" box over the [visible] ids. */
export function visibleSelectionState(
  selected: readonly string[],
  visible: readonly string[]
): boolean | "indeterminate" {
  if (visible.length === 0) return false;
  const picked = new Set(selected);
  const count = visible.filter((id) => picked.has(id)).length;
  if (count === 0) return false;
  return count === visible.length ? true : "indeterminate";
}

/** [selected] with all [visible] ids added ([checked]) or removed, keeping the order of the picks. */
export function withVisible(
  selected: readonly string[],
  visible: readonly string[],
  checked: boolean
): string[] {
  if (checked) {
    const picked = new Set(selected);
    return [...selected, ...visible.filter((id) => !picked.has(id))];
  }
  const hidden = new Set(visible);
  return selected.filter((id) => !hidden.has(id));
}

/** [selected] with [id] flipped. */
export function toggled(selected: readonly string[], id: string): string[] {
  return selected.includes(id)
    ? selected.filter((it) => it !== id)
    : [...selected, id];
}
