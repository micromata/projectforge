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

import 'dart:io' as io;

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

/// The cookies of the WebView, in which the app shows the ProjectForge pages: the web session opened by the app token
/// (`POST /rs/appAuth/webSession`) is handed over here, and the app's logout removes it.
///
/// No JavaScript handlers are registered in the WebView: the pages must never get hold of the app token.
class WebCookies {
  /// Sets the cookies of a response's `set-cookie` headers for [serverUrl] in the WebView.
  Future<void> setCookies(
    String serverUrl,
    List<String> setCookieHeaders,
  ) async {
    final url = WebUri(serverUrl);
    for (final cookie in parseSetCookies(setCookieHeaders)) {
      await CookieManager.instance().setCookie(
        url: url,
        name: cookie.name,
        value: cookie.value,
        path: cookie.path ?? '/',
        isSecure: cookie.secure,
        isHttpOnly: cookie.httpOnly,
        sameSite: _sameSite(cookie.sameSite),
      );
    }
  }

  /// The `Cookie` header of the WebView for [url] (its web session), for a download the app does in place of the
  /// WebView. Null if there is none.
  Future<String?> cookieHeader(Uri url) async {
    final cookies = await CookieManager.instance().getCookies(
      url: WebUri.uri(url),
    );
    final header = cookies
        .where((it) => '${it.value}'.isNotEmpty)
        .map((it) => '${it.name}=${it.value}')
        .join('; ');
    return header.isEmpty ? null : header;
  }

  /// Removes all cookies and the cache of the WebView (logout of the app).
  Future<void> clear() async {
    try {
      await CookieManager.instance().deleteAllCookies();
      await InAppWebViewController.clearAllCache();
    } on MissingPluginException {
      // No WebView platform (tests).
    }
  }

  static HTTPCookieSameSitePolicy _sameSite(io.SameSite? sameSite) =>
      switch (sameSite) {
        io.SameSite.strict => HTTPCookieSameSitePolicy.STRICT,
        io.SameSite.none => HTTPCookieSameSitePolicy.NONE,
        _ => HTTPCookieSameSitePolicy.LAX,
      };

  /// The cookies of `set-cookie` headers; unparsable or deleting ones (empty value) are skipped.
  @visibleForTesting
  static List<io.Cookie> parseSetCookies(List<String> setCookieHeaders) {
    final cookies = <io.Cookie>[];
    for (final header in setCookieHeaders) {
      try {
        final cookie = io.Cookie.fromSetCookieValue(header);
        if (cookie.value.isNotEmpty) cookies.add(cookie);
      } on Exception {
        // Not a cookie the WebView needs.
      }
    }
    return cookies;
  }
}
