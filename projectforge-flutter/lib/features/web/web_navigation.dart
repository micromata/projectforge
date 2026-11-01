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

/// Where a navigation of the WebView goes, decided by [WebNavigation.decide].
enum WebNavigationTarget {
  /// A page of the own server: stays in the WebView.
  webView,

  /// Another site, mail, phone: opened by the system (browser, mail app, …).
  external,

  /// Nothing to open (an unknown scheme).
  cancel,
}

/// The rules for the navigation of the WebView with the pages of [server], without any WebView, so they can be
/// tested.
class WebNavigation {
  WebNavigation(this.server);

  final Uri server;

  /// The start page of the pages in the app if nothing else is given (`/next/`).
  static const defaultPath = '/next/';

  static const _loginPath = '/next/login';
  static const _externalSchemes = {'mailto', 'tel', 'sms'};
  static const _inlineSchemes = {'about', 'blob', 'data'};

  WebNavigationTarget decide(Uri target) {
    if (target.scheme == 'http' || target.scheme == 'https') {
      return isSameOrigin(target)
          ? WebNavigationTarget.webView
          : WebNavigationTarget.external;
    }
    if (_externalSchemes.contains(target.scheme)) {
      return WebNavigationTarget.external;
    }
    if (_inlineSchemes.contains(target.scheme)) {
      return WebNavigationTarget.webView;
    }
    return WebNavigationTarget.cancel;
  }

  bool isSameOrigin(Uri url) =>
      url.scheme == server.scheme &&
      url.host == server.host &&
      url.port == server.port;

  /// The login page of next: the web session is gone (timeout, server restart), the app opens a new one.
  bool isLoginPage(Uri url) =>
      isSameOrigin(url) &&
      (url.path == _loginPath || url.path == '$_loginPath/');

  /// The page to return to after a new web session: the `returnUrl` of the login page (a path below `/next`, as
  /// built by next's AuthGuard), or [defaultPath]. Only a plain path of the own server, never another host.
  String returnPath(Uri loginUrl) {
    final returnUrl = loginUrl.queryParameters['returnUrl'];
    if (returnUrl == null ||
        !returnUrl.startsWith('/') ||
        returnUrl.startsWith('//') ||
        returnUrl.contains('\\') ||
        returnUrl.startsWith('/login')) {
      return defaultPath;
    }
    return '/next$returnUrl';
  }

  /// The full url of a [path] of the server.
  Uri url(String path) => server.resolve(path);
}
