/** Rest endpoint prefix of the Spring backend. Relative action urls are resolved against it. */
const REST_PREFIX = "/rs/";

/**
 * Resolves an action url onto a path this app can fetch.
 *
 * The backend hands out rest urls relative to its rest base (`book/save`), mirroring
 * `getServiceURL` of the legacy app; an url that already starts with a slash is origin-relative
 * and taken as is. Absolute urls (`https://…`) are left untouched - the caller opens those in a
 * new tab rather than fetching them.
 */
export function resolveRestUrl(url: string): string {
  if (/^[a-z][a-z0-9+.-]*:\/\//i.test(url)) return url;
  return url.startsWith("/") ? url : `${REST_PREFIX}${url}`;
}
