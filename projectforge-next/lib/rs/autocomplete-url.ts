/**
 * The `maxResults` of a lookup url (see fetchAutoCompletion).
 *
 * Its own module because it is pure and worth a test: the backend has no offset parameter on
 * `{category}/autosearch`, so a picker that loads more while the user scrolls can only ask again for
 * a larger cut — the cap is the whole of its paging.
 */

/** How many entries a picker asks for at a time. */
export const LOOKUP_PAGE_SIZE = 50;

/**
 * The url template with `maxResults` set to [maxResults], replacing one that is already there.
 *
 * Replacing rather than appending: the backend hands out urls that carry their own cap
 * (`AbstractPagesRest.quickSelectUrl` is `…/autosearch?maxResults=30&search=:search`), and a second
 * occurrence of the parameter would not bind to Spring's `Int`. The `:search` placeholder is left
 * untouched — fetchAutoCompletion fills it in.
 */
export function withMaxResults(url: string, maxResults: number): string {
  const replaced = url.replace(
    /([?&])maxResults=[^&]*/,
    `$1maxResults=${maxResults}`
  );
  if (replaced !== url) return replaced;
  return `${url}${url.includes("?") ? "&" : "?"}maxResults=${maxResults}`;
}
