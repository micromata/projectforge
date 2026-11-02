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

/// A cookie of the WebView, see [WebCookies.cookies].
typedef WebViewCookie = ({String name, String value, String? domain});

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

  /// Sets the cookies of an SSO proxy in front of the server (name to value, see `Session.proxyCookies`) in the
  /// WebView: otherwise the proxy shows its login instead of the pages.
  Future<void> setProxyCookies(
    String serverUrl,
    Map<String, String> cookies,
  ) async {
    if (cookies.isEmpty) return;
    try {
      final url = WebUri(serverUrl);
      for (final cookie in cookies.entries) {
        await CookieManager.instance().setCookie(
          url: url,
          name: cookie.key,
          value: cookie.value,
          path: '/',
          isSecure: url.scheme == 'https',
          isHttpOnly: true,
        );
      }
    } on MissingPluginException {
      // No WebView platform (tests).
    }
  }

  /// All cookies of the WebView for [serverUrl] (name and value; a name may occur twice, e.g. for the host and for
  /// its domain), empty without a WebView platform (tests). [domain] is the domain a cookie was set for, null if the
  /// platform doesn't tell (Android).
  Future<List<WebViewCookie>> cookies(String serverUrl) async {
    try {
      final cookies = await CookieManager.instance().getCookies(
        url: WebUri(serverUrl),
      );
      return [
        for (final cookie in cookies)
          (
            name: cookie.name,
            value: '${cookie.value ?? ''}',
            domain: cookie.domain,
          ),
      ];
    } on MissingPluginException {
      return const [];
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

  /// Removes all cookies, the cache and the web storage (localStorage, IndexedDB, …) of the WebView (logout of the
  /// app): nothing of the pages stays on the device.
  Future<void> clear() async {
    try {
      await CookieManager.instance().deleteAllCookies();
      await InAppWebViewController.clearAllCache();
      final storage = WebStorageManager.instance();
      if (defaultTargetPlatform == TargetPlatform.android) {
        await storage.deleteAllData();
      } else if (defaultTargetPlatform == TargetPlatform.iOS) {
        await storage.removeDataModifiedSince(
          dataTypes: WebsiteDataType.ALL,
          date: DateTime.fromMillisecondsSinceEpoch(0),
        );
      }
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
