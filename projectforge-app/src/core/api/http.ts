/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

/**
 * The fetch of the app: `expo/fetch` (a native implementation), not the global fetch of React Native. Only it has
 * `redirect: 'manual'` (the global one always follows a redirect, also with the `Authorization` header or cookies to
 * another host) and `credentials: 'omit'` that really keeps the native cookie jar out (on Android the jar of the
 * global fetch is the WebView's CookieManager, so the web session would go along with every request of the token).
 *
 * Every request of the app passes [HTTP_DEFAULTS]: no cookie jar, no redirects.
 */
export type HttpFetch = (url: string, init: RequestInit) => Promise<Response>;

export const HTTP_DEFAULTS: Pick<RequestInit, 'credentials' | 'redirect'> = {
  credentials: 'omit',
  redirect: 'manual',
};

/**
 * The values of the `Set-Cookie` headers of [response]. The Headers of React Native have no `getSetCookie()` and join
 * several headers with commas (iOS even does so natively, HTTPURLResponse), so they are split again: at a comma
 * followed by `name=`, not at the comma of a date (`Expires=Wed, 21 Oct 2026 …`).
 */
export function setCookieHeaders(response: Response): string[] {
  const headers = response.headers as Headers & { getSetCookie?: () => string[] };
  const list = headers.getSetCookie?.();
  if (list && list.length) return list;
  return splitSetCookie(headers.get('set-cookie'));
}

export function splitSetCookie(joined: string | null | undefined): string[] {
  if (!joined) return [];
  return joined
    .split(/,(?=\s*[^;,=\s]+=)/)
    .map((it) => it.trim())
    .filter((it) => it.length > 0);
}

/** A status of a redirect, also the `opaqueredirect` (status 0) of a WHATWG fetch with `redirect: 'manual'`. */
export function isRedirect(response: Response): boolean {
  return (response.status >= 300 && response.status < 400) || response.type === 'opaqueredirect';
}
