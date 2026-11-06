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

import { parseUrl, SafeURL } from '../../core/url';

/** Where a navigation of the WebView goes, decided by [WebNavigation.decide]. */
export type WebNavigationTarget =
  /** A page of the own server: stays in the WebView. */
  | 'webView'
  /** Another site the user went to, mail, phone: opened by the system (browser, mail app, …). */
  | 'external'
  /** Nothing to open (an unknown scheme). */
  | 'cancel';

/** The pages the app has a native tab for (the Flutter app's NativePage). */
export type NativePage = 'chat' | 'businessCards' | 'notifications' | 'addresses' | 'timesheets';

const LOGIN_PATH = '/next/login';
const EXTERNAL_SCHEMES = new Set(['mailto:', 'tel:', 'sms:']);

/** Not `data:`: a page of its own, which the app (without an address bar) would show like one of the server. */
const INLINE_SCHEMES = new Set(['about:', 'blob:']);

const isHttp = (url: URL) => url.protocol === 'http:' || url.protocol === 'https:';

/**
 * The rules for the navigation of the WebView with the pages of [server], without any WebView, so they can be
 * tested. Port of the Flutter app's `lib/features/web/web_navigation.dart`; every url is parsed WHATWG conformant
 * (src/core/url.ts), as the WebView does.
 */
export class WebNavigation {
  /** The start page of the pages in the app if nothing else is given. */
  static readonly defaultPath = '/next/';

  readonly server: URL;

  constructor(server: string | URL) {
    this.server = typeof server === 'string' ? new SafeURL(server) : server;
  }

  /**
   * Another site opens in the system browser, never in the WebView (it has no address bar), also the login of an
   * SSO proxy ([isProxyLogin]), which the app renews in the system browser instead.
   */
  decide(target: string | URL): WebNavigationTarget {
    const url = this.parse(target);
    if (url == null) return 'cancel';
    if (isHttp(url)) return this.isSameOrigin(url) ? 'webView' : 'external';
    if (EXTERNAL_SCHEMES.has(url.protocol)) return 'external';
    if (INLINE_SCHEMES.has(url.protocol)) return 'webView';
    return 'cancel';
  }

  /**
   * A navigation to another site is the login of the SSO proxy in front of the server: a redirect while a page of
   * the server loads (the proxy refused it and sends the browser to its login). Not a script of a loaded page (an
   * external entry of the menu) and not a link the user tapped, which go to the system browser. Only behind a proxy
   * ([behindProxy]: the server named proxy cookies at the login).
   */
  isProxyLogin(
    target: string | URL,
    opts: { redirect: boolean; loadingServerPage: boolean; behindProxy: boolean },
  ): boolean {
    const url = this.parse(target);
    return (
      url != null &&
      isHttp(url) &&
      !this.isSameOrigin(url) &&
      opts.redirect &&
      opts.loadingServerPage &&
      opts.behindProxy
    );
  }

  isSameOrigin(target: string | URL): boolean {
    const url = this.parse(target);
    return (
      url != null &&
      url.protocol === this.server.protocol &&
      url.hostname === this.server.hostname &&
      url.port === this.server.port
    );
  }

  /** A web page of another site (http or https), never to be shown in the WebView. */
  isForeignPage(target: string | URL): boolean {
    const url = this.parse(target);
    return url != null && isHttp(url) && !this.isSameOrigin(url);
  }

  /** The login page of next: the web session is gone (timeout, server restart), the app opens a new one. */
  isLoginPage(target: string | URL): boolean {
    const url = this.parse(target);
    return (
      url != null &&
      this.isSameOrigin(url) &&
      (url.pathname === LOGIN_PATH || url.pathname === `${LOGIN_PATH}/`)
    );
  }

  /** A page of next (below `/next`), not a legacy page (`/wa/`). */
  isNextPage(target: string | URL): boolean {
    const url = this.parse(target);
    return (
      url != null &&
      this.isSameOrigin(url) &&
      (url.pathname === '/next' || url.pathname.startsWith('/next/'))
    );
  }

  /** The page of the server the app has a native tab for, null for other pages and other origins. */
  nativePage(target: string | URL): NativePage | null {
    const url = this.parse(target);
    if (url == null || !this.isSameOrigin(url)) return null;
    const path = url.pathname;
    if (path === '/next/chat' || path.startsWith('/next/chat/')) return 'chat';
    if (path === '/next/myBusinessCards' || path === '/next/myBusinessCards/') return 'businessCards';
    if (path === '/next/notifications' || path === '/next/notifications/') return 'notifications';
    if (path === '/next/address' || path === '/next/address/') return 'addresses';
    if (path === '/next/timesheet' || path === '/next/timesheet/') return 'timesheets';
    return null;
  }

  /**
   * The page to return to after a new web session: the `returnUrl` of the login page (a path below `/next`, as
   * built by next's AuthGuard), or [defaultPath]. Only a plain path of the own server, never another host.
   */
  returnPath(loginUrl: string | URL): string {
    const url = this.parse(loginUrl);
    const returnUrl = url?.searchParams.get('returnUrl');
    if (
      returnUrl == null ||
      !returnUrl.startsWith('/') ||
      returnUrl.startsWith('//') ||
      returnUrl.includes('\\') ||
      returnUrl.startsWith('/login')
    ) {
      return WebNavigation.defaultPath;
    }
    return `/next${returnUrl}`;
  }

  /** The full url of a [path] of the server. */
  url(path: string): URL {
    return new SafeURL(path, this.server);
  }

  /**
   * The page of the server at [path] (a path, or a url of the server), null if it resolves to another site (e.g.
   * `//other.example.org/`, which the WebView would load without asking [decide] on Android).
   */
  pageUrl(path: string): URL | null {
    const url = parseUrl(path, this.server.href);
    return url != null && this.isSameOrigin(url) ? url : null;
  }

  private parse(target: string | URL): URL | null {
    return typeof target === 'string' ? parseUrl(target) : target;
  }
}
