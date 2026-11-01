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
class WebSessionRepository {
  WebSessionRepository(this._api, this._cookies);

  final ApiClient _api;
  final WebCookies _cookies;

  /// Opens a new web session; a second factor too old for it is renewed in the browser by the [ApiClient].
  ///
  /// @return The path of the start page.
  Future<String> open() async {
    final response = await _api.postForResponse<Map<String, dynamic>>(
      '/rs/appAuth/webSession',
    );
    await _cookies.setCookies(
      _api.serverUrl,
      response.headers['set-cookie'] ?? const [],
    );
    return response.data?['startUrl'] as String? ?? WebNavigation.defaultPath;
  }
}
