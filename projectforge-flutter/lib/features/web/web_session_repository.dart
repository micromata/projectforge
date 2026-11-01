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

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_client.dart';
import '../../core/auth/auth_controller.dart';
import '../../core/web/web_cookies.dart';
import 'web_navigation.dart';

final webSessionRepositoryProvider = Provider<WebSessionRepository?>((ref) {
  final api = ref.watch(apiClientProvider);
  if (api == null) return null;
  return WebSessionRepository(api, ref.read(webCookiesProvider));
});

/// The web session of the WebView (AppAuthRest.webSession, `POST /rs/appAuth/webSession`): the server opens it for
/// the app token and sends its cookie, which goes into the WebView.
///
/// All WebViews of the app (the tabs "ProjectForge" and "Chat") share one cookie jar and so one session: a tab
/// takes the opened one ([ensure]), a renewal is done once for all of them ([renew]). The repository lives as long as
/// the login (it is rebuilt with the [ApiClient]).
class WebSessionRepository {
  WebSessionRepository(this._api, this._cookies);

  final ApiClient _api;
  final WebCookies _cookies;
  Future<String>? _session;
  Future<String>? _pending;

  /// The opened web session, or a new one if none was opened yet (or the last attempt failed).
  ///
  /// @return The path of the start page.
  Future<String> ensure() => _session ??= _open();

  /// A new web session (the old one is gone: timeout, server restart). Concurrent renewals share one request.
  ///
  /// @return The path of the start page.
  Future<String> renew() {
    final pending = _pending;
    if (pending != null) return pending;
    final renewal = _open();
    _pending = renewal;
    _session = renewal;
    return renewal.whenComplete(() => _pending = null);
  }

  /// Opens a new web session; a second factor too old for it is renewed in the browser by the [ApiClient]. A failed
  /// attempt isn't kept, the next [ensure] tries again.
  Future<String> _open() async {
    try {
      final response = await _api.postForResponse<Map<String, dynamic>>(
        '/rs/appAuth/webSession',
      );
      await _cookies.setProxyCookies(_api.serverUrl, _api.proxyCookies());
      await _cookies.setCookies(
        _api.serverUrl,
        response.headers['set-cookie'] ?? const [],
      );
      return response.data?['startUrl'] as String? ?? WebNavigation.defaultPath;
    } on Object {
      _session = null;
      rethrow;
    }
  }
}
