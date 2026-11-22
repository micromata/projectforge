/**
 * The route (without base path) of [pathname] with [params] and the entries of [changes] applied: a string sets
 * the param, null removes it; all others stay.
 */
export function hrefWithParams(
  pathname: string,
  params: URLSearchParams,
  changes: Record<string, string | null>
): string {
  const next = new URLSearchParams(params);
  for (const [key, value] of Object.entries(changes)) {
    if (value === null) next.delete(key);
    else next.set(key, value);
  }
  const query = next.toString();
  return query ? `${pathname}?${query}` : pathname;
}
