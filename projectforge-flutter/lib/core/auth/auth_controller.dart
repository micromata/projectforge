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
import '../cache/shared_files.dart';
import '../log/app_log.dart';
import '../log/log_sanitizer.dart';
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
final sharedFilesProvider = Provider<SharedFiles>((ref) => SharedFiles());
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
    proxyCookies: () =>
        ref.read(authControllerProvider).session?.proxyCookies ?? const {},
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
    if (session != null) {
      LogSanitizer.addSecretNames(session.proxyCookies.keys);
    }
    appLog.info(
      'auth',
      session == null
          ? 'No stored login'
          : 'Stored login: user ${session.userId}, device ${session.deviceId}, proxy cookies: ${session.proxyCookies.length}',
    );
    state = session == null
        ? const AuthState(AuthStatus.loggedOut)
        : AuthState(AuthStatus.loggedIn, session: session);
  }

  /// The servers of the last logins, most recent first (offered by the login).
  Future<List<String>> servers() => ref.read(sessionStoreProvider).servers();

  Future<void> removeServer(String serverUrl) =>
      ref.read(sessionStoreProvider).removeServer(serverUrl);

  /// Logs in at [serverInput] (as typed by the user) in the browser.
  ///
  /// Throws [InvalidServerUrl], [InsecureServerUrl] for plain http in a release build, [AppLoginCancelled] if the user cancelled, [ApiException] if the server isn't a reachable
  /// ProjectForge or refused the code.
  Future<void> login(String serverInput) async {
    final serverUrl = AppLogin.normalizeServerUrl(serverInput);
    if (serverUrl == null) throw InvalidServerUrl();
    if (AppLogin.isInsecure(serverUrl)) throw InsecureServerUrl();
    final appLogin = ref.read(appLoginProvider);
    final Session session;
    try {
      await appLogin.systemStatus(serverUrl);
      final device = await ref.read(deviceDescriptionProvider.future);
      appLog.info(
        'auth',
        'Login (app ${device.appVersion}, ${device.platform})',
      );
      session = await appLogin.login(
        serverUrl: serverUrl,
        deviceName: device.name,
        platform: device.platform,
      );
    } on Object catch (e, stack) {
      if (e is! AppLoginCancelled) {
        appLog.error('auth', 'Login failed', e, stack);
      }
      rethrow;
    }
    final expired = _expiredLogin;
    _expiredLogin = null;
    if (expired != null && expired != _loginKey(session)) {
      _clearLog('Logged in as another user: log of the expired login cleared');
    }
    await ref.read(sessionStoreProvider).save(session);
    state = AuthState(AuthStatus.loggedIn, session: session);
  }

  /// The login whose expiry left its log ([onUnauthorized]): cleared once somebody else logs in.
  String? _expiredLogin;

  static String _loginKey(Session session) =>
      '${session.serverUrl}#${session.userId}';

  /// A new login of this app in the browser, because its second factor is too old for a request. Concurrent
  /// requests share one login.
  ///
  /// @return false if the user cancelled (the request fails then) or it failed.
  Future<bool> reauthenticate() => _reauthentication ??= _reauthenticate()
      .whenComplete(() => _reauthentication = null);

  Future<bool> _reauthenticate() async {
    final session = state.session;
    if (session == null) return false;
    appLog.info('auth', 'Renewal of the login');
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
      if (renewed.userId != session.userId ||
          renewed.deviceId != session.deviceId) {
        appLog.warn(
          'auth',
          'Renewal logged in user ${renewed.userId}/device ${renewed.deviceId} instead of '
              '${session.userId}/${session.deviceId}: switched',
        );
        await _switchTo(renewed);
        return false;
      }
      await ref.read(sessionStoreProvider).save(renewed);
      state = AuthState(AuthStatus.loggedIn, session: renewed);
      // The web session stays, but the WebView needs the fresh cookies of an SSO proxy.
      await ref
          .read(webCookiesProvider)
          .setProxyCookies(renewed.serverUrl, renewed.proxyCookies);
      appLog.info('auth', 'Login renewed');
      return true;
    } on AppLoginCancelled {
      appLog.info('auth', 'Renewal cancelled');
      return false;
    } on Object catch (e, stack) {
      appLog.error('auth', 'Renewal failed', e, stack);
      return false;
    }
  }

  /// The WebView passed the login of the SSO proxy (its session had expired, the proxy redirected the pages to its
  /// login): its fresh cookies replace the stored ones, so the requests of the app get through again.
  ///
  /// Only the cookies the server named as proxy cookies at the login are taken (`Session.proxyCookies`), never others
  /// of the WebView, and only those of the server's host: a cookie of the same name set for the whole domain may come
  /// from another site of that domain. A name the WebView has twice is taken with the value that differs from the
  /// stored, expired one; if both differ and the platform doesn't tell their domains (Android), none is taken.
  ///
  /// @param webViewCookies The cookies of the WebView for the server.
  /// @return true if a cookie was renewed.
  Future<bool> takeProxyCookies(List<WebViewCookie> webViewCookies) async {
    final session = state.session;
    if (session == null || session.proxyCookies.isEmpty) return false;
    final host = Uri.parse(session.serverUrl).host;
    final renewed = <String, String>{};
    for (final MapEntry(key: name, value: stored)
        in session.proxyCookies.entries) {
      final values = webViewCookies
          .where(
            (it) =>
                it.name == name &&
                it.value.isNotEmpty &&
                _ofHost(it.domain, host),
          )
          .map((it) => it.value)
          .toList();
      if (values.length > 1) {
        appLog.warn(
          'auth',
          'The WebView has ${values.length} cookies $name (host and domain)',
        );
      }
      final fresh = values.where((it) => it != stored).toSet();
      if (fresh.length > 1) {
        // One of them may be set for the whole domain by another site.
        appLog.warn('auth', 'Cookie $name ambiguous: not taken');
      } else if (fresh.length == 1) {
        renewed[name] = fresh.single;
      }
    }
    if (renewed.isEmpty) {
      appLog.debug('auth', 'Proxy cookies of the WebView unchanged');
      return false;
    }
    final updated = session.withProxyCookies({
      ...session.proxyCookies,
      ...renewed,
    });
    await ref.read(sessionStoreProvider).save(updated);
    state = AuthState(AuthStatus.loggedIn, session: updated);
    ref.read(apiClientProvider)?.proxyCookiesRenewed();
    appLog.info(
      'auth',
      'Proxy cookies renewed by the login in the WebView: ${renewed.keys.join(', ')}',
    );
    return true;
  }

  /// A cookie set for [host] itself, not for a parent domain (`.example.org`). Null: the platform doesn't tell
  /// (Android), taken.
  static bool _ofHost(String? domain, String host) =>
      domain == null ||
      domain.isEmpty ||
      domain.toLowerCase() == host.toLowerCase();

  /// The browser was logged in as another user, for whom the server registered this app anew: the old user is logged
  /// out (at the server too, best effort), and nothing of them stays (cache, cookies, tabs). The request that asked
  /// for the renewal was the old user's and fails.
  Future<void> _switchTo(Session renewed) async {
    try {
      await ref
          .read(apiClientProvider)
          ?.post<void>('/rs/appAuth/logout', renewLogin: false);
    } on ApiException {
      // The old token expires anyway.
    }
    await _clearLocalData();
    _clearLog('Logged in as another user: log of the previous user cleared');
    await ref.read(sessionStoreProvider).save(renewed);
    state = AuthState(AuthStatus.loggedIn, session: renewed);
  }

  /// Logs the app out at the server (its token is deleted) and here.
  ///
  /// The server gets a few seconds and whatever fails: the app is logged out here in any case, so the user reaches
  /// the login (and another server) even if the server isn't available.
  Future<void> logout() async {
    appLog.info('auth', 'Logout');
    try {
      await ref
          .read(apiClientProvider)
          ?.post<void>('/rs/appAuth/logout', renewLogin: false)
          .timeout(const Duration(seconds: 3));
    } on Object {
      // Offline, timed out or already logged out: the token is forgotten here anyway.
    }
    await _forget(null);
    _clearLog('Logged out: log of the login cleared');
  }

  /// The log of a login isn't left to whoever uses the device next (it names the server, the pages, the ids). Kept
  /// for an expired login: it tells why, and the same user logs in again.
  void _clearLog(String reason) => appLog
    ..clear()
    ..info('auth', reason);

  /// The server refused the token (logged out in the web, expired, password changed).
  void onUnauthorized() {
    final session = state.session;
    if (state.status != AuthStatus.loggedIn || session == null) return;
    _expiredLogin = _loginKey(session);
    _forget('app.session.expired');
  }

  Future<void> _forget(String? messageKey) async {
    await ref.read(sessionStoreProvider).clear();
    await _clearLocalData();
    state = AuthState(AuthStatus.loggedOut, messageKey: messageKey);
  }

  /// The user's data on the device: the cache, the files handed to the share sheet, and the web session and storage
  /// of the WebView (its server side ends with the app's token, LoginService.appWebLogin).
  Future<void> _clearLocalData() async {
    await ref.read(jsonCacheProvider).clearAll();
    await ref.read(sharedFilesProvider).clearAll();
    await ref.read(webCookiesProvider).clear();
  }
}
