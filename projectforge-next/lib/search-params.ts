/** Changes of search parameters: a value sets the parameter, `null` removes it. */
export type SearchParamChanges = Record<string, string | null>;

/**
 * The search part (`?a=1`, or `""` without any parameter) of [search] with the [changes] applied; the other
 * parameters are kept.
 */
export function withSearchParams(
  search: string,
  changes: SearchParamChanges
): string {
  const query = new URLSearchParams(search);
  for (const [name, value] of Object.entries(changes)) {
    if (value === null) query.delete(name);
    else query.set(name, value);
  }
  const result = query.toString();
  return result ? `?${result}` : "";
}

/**
 * Changes search parameters of the current url - the page's view state (open tab, a filter), so that a reload or a
 * shared link shows the same view. `push` adds a history entry, so that the browser's back button returns to the
 * previous view; `replace` doesn't (a one-time parameter dropped after use, stepping through months).
 *
 * The native History API, not `router.push`: Next integrates it (`useSearchParams` follows), and `router.push`
 * doesn't arrive on a deep link of the static export (see EditPageShell.selectSection).
 */
export function updateSearchParams(
  changes: SearchParamChanges,
  mode: "push" | "replace"
): void {
  const url =
    withSearchParams(window.location.search, changes) ||
    window.location.pathname;
  if (mode === "push") window.history.pushState(null, "", url);
  else window.history.replaceState(null, "", url);
}
