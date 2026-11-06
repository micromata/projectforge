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

import { parseUrl } from '../url';

/** The native side (modules/pf-web-support), replaced in the tests. */
export interface WebCookieStore {
  setCookies(url: string, setCookieHeaders: string[]): Promise<string[]>;
  cookieHeader(url: string): Promise<string | null>;
  clearAll(): Promise<void>;
}

/**
 * The cookies of the WebView, in which the app shows the ProjectForge pages (the Flutter app's
 * `lib/core/web/web_cookies.dart`): the web session opened by the app token (`POST /rs/appAuth/webSession`) is
 * handed over here, and the app's logout removes it. No JavaScript handlers of the app are reachable for the
 * pages: they never get hold of the app token.
 */
export class WebCookies {
  constructor(private readonly store: WebCookieStore) {}

  /** Sets the cookies of a response's `set-cookie` headers for [serverUrl] in the WebView; answers their names. */
  setCookies(serverUrl: string, setCookieHeaders: string[]): Promise<string[]> {
    return this.store.setCookies(serverUrl, setCookieHeaders);
  }

  /**
   * Sets the cookies of an SSO proxy in front of the server (name to value, see `Session.proxyCookies`) in the
   * WebView: otherwise the proxy shows its login instead of the pages.
   */
  async setProxyCookies(serverUrl: string, cookies: Record<string, string>): Promise<void> {
    const entries = Object.entries(cookies);
    if (!entries.length) return;
    const secure = parseUrl(serverUrl)?.protocol === 'https:';
    await this.store.setCookies(
      serverUrl,
      entries.map(([name, value]) => `${name}=${value}; Path=/; HttpOnly${secure ? '; Secure' : ''}`),
    );
  }

  /** The `Cookie` header of the WebView for [url] (its web session), for a download the app does itself. */
  cookieHeader(url: string): Promise<string | null> {
    return this.store.cookieHeader(url);
  }

  /** Removes all cookies, the cache and the web storage of the WebView (logout): nothing of the pages stays. */
  clear(): Promise<void> {
    return this.store.clearAll();
  }
}
