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

import '../api/api_client.dart';
import '../api/api_exception.dart';
import '../cache/json_cache.dart';
import '../web/web_cookies.dart';
import 'app_login.dart';
import 'device_description.dart';
import 'session.dart';

enum AuthStatus { restoring, loggedOut, loggedIn }

class AuthState {
  const AuthState(this.status, {this.session, this.messageKey});

  final AuthStatus status;
  final Session? session;

  /// Why the app was logged out (an i18n key), shown on the login screen.
  final String? messageKey;
}

final sessionStoreProvider = Provider<SessionStore>((ref) => SessionStore());
final appLoginProvider = Provider<AppLogin>((ref) => AppLogin());
final jsonCacheProvider = Provider<JsonCache>((ref) => JsonCache());
final webCookiesProvider = Provider<WebCookies>((ref) => WebCookies());
final deviceDescriptionProvider = FutureProvider<DeviceDescription>(
  (ref) => DeviceDescription.load(),
);

final authControllerProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);

/// The client of the logged-in app, null while logged out. Rebuilt only for another login (device), not for a
/// renewed token: the client reads the token per request.
final apiClientProvider = Provider<ApiClient?>((ref) {
  final key = ref.watch(
    authControllerProvider.select(
      (it) => '${it.session?.serverUrl}#${it.session?.deviceId}',
    ),
  );
  final session = ref.read(authControllerProvider).session;
  if (session == null || key.isEmpty) return null;
  final controller = ref.read(authControllerProvider.notifier);
  return ApiClient(
    serverUrl: session.serverUrl,
    token: () => ref.read(authControllerProvider).session?.token,
    appVersion: ref.read(deviceDescriptionProvider).value?.appVersion ?? '',
    onUnauthorized: controller.onUnauthorized,
    onReauthenticate: controller.reauthenticate,
  );
});

/// Login, logout and the renewal of the login (a new second factor) of the app.
class AuthController extends Notifier<AuthState> {
  Future<bool>? _reauthentication;

  @override
  AuthState build() {
    _restore();
    return const AuthState(AuthStatus.restoring);
  }

  Future<void> _restore() async {
    final session = await ref.read(sessionStoreProvider).load();
    state = session == null
        ? const AuthState(AuthStatus.loggedOut)
        : AuthState(AuthStatus.loggedIn, session: session);
  }

  Future<String?> lastServerUrl() =>
      ref.read(sessionStoreProvider).lastServerUrl();

  /// Logs in at [serverInput] (as typed by the user) in the browser.
  ///
  /// Throws [InvalidServerUrl], [AppLoginCancelled] if the user cancelled, [ApiException] if the server isn't a reachable
  /// ProjectForge or refused the code.
  Future<void> login(String serverInput) async {
    final serverUrl = AppLogin.normalizeServerUrl(serverInput);
    if (serverUrl == null) throw InvalidServerUrl();
    final appLogin = ref.read(appLoginProvider);
    await appLogin.systemStatus(serverUrl);
    final device = await ref.read(deviceDescriptionProvider.future);
    final session = await appLogin.login(
      serverUrl: serverUrl,
      deviceName: device.name,
      platform: device.platform,
    );
    await ref.read(sessionStoreProvider).save(session);
    state = AuthState(AuthStatus.loggedIn, session: session);
  }

  /// A new login of this app in the browser, because its second factor is too old for a request. Concurrent
  /// requests share one login.
  ///
  /// @return false if the user cancelled (the request fails then) or it failed.
  Future<bool> reauthenticate() => _reauthentication ??= _reauthenticate()
      .whenComplete(() => _reauthentication = null);

  Future<bool> _reauthenticate() async {
    final session = state.session;
    if (session == null) return false;
    try {
      final device = await ref.read(deviceDescriptionProvider.future);
      final renewed = await ref
          .read(appLoginProvider)
          .login(
            serverUrl: session.serverUrl,
            deviceName: device.name,
            platform: device.platform,
            deviceId: session.deviceId,
          );
      await ref.read(sessionStoreProvider).save(renewed);
      state = AuthState(AuthStatus.loggedIn, session: renewed);
      return true;
    } on Object {
      return false;
    }
  }

  /// Logs the app out at the server (its token is deleted) and here.
  Future<void> logout() async {
    try {
      await ref.read(apiClientProvider)?.post<void>('/rs/appAuth/logout');
    } on ApiException {
      // Offline or already logged out: the token is forgotten here anyway.
    }
    await _forget(null);
  }

  /// The server refused the token (logged out in the web, expired, password changed).
  void onUnauthorized() {
    if (state.status == AuthStatus.loggedIn) _forget('app.session.expired');
  }

  Future<void> _forget(String? messageKey) async {
    await ref.read(sessionStoreProvider).clear();
    await ref.read(jsonCacheProvider).clearAll();
    // The web session of the WebView: its server side ends with the app's token (LoginService.appWebLogin).
    await ref.read(webCookiesProvider).clear();
    state = AuthState(AuthStatus.loggedOut, messageKey: messageKey);
  }
}
