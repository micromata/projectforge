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

import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/api/api_client.dart';
import 'package:projectforge_app/core/auth/app_login.dart';
import 'package:projectforge_app/core/auth/auth_controller.dart';
import 'package:projectforge_app/core/auth/device_description.dart';
import 'package:projectforge_app/core/auth/session.dart';
import 'package:projectforge_app/core/cache/json_cache.dart';
import 'package:projectforge_app/core/cache/shared_files.dart';
import 'package:projectforge_app/core/log/app_log.dart';
import 'package:projectforge_app/core/web/web_cookies.dart';

import 'fake_adapter.dart';

class _MemorySessionStore extends SessionStore {
  _MemorySessionStore(this.session);

  Session? session;

  @override
  Future<Session?> load() async => session;

  @override
  Future<void> save(Session session) async => this.session = session;

  @override
  Future<void> clear() async => session = null;
}

class _FakeWebCookies extends WebCookies {
  int cleared = 0;

  @override
  Future<void> clear() async => cleared++;
}

void main() {
  const serverUrl = 'https://pf.example.com';
  const userA = Session(
    serverUrl: serverUrl,
    token: 'tokenA',
    userId: 1,
    deviceId: 10,
  );

  late Directory dir;
  late _MemorySessionStore store;
  late _FakeWebCookies cookies;
  late FakeAdapter api;

  /// The requests of the browser login (systemStatus, token).
  late FakeAdapter loginApi;

  /// The browser login answers [userId] and [deviceId] (the server's answer for the user logged in there).
  ProviderContainer container({required int userId, required int deviceId}) {
    loginApi = FakeAdapter(
      (_) => (
        status: 200,
        body: {'userId': userId, 'deviceId': deviceId, 'token': 'new'},
      ),
    );
    final login = AppLogin(
      dio: Dio()..httpClientAdapter = loginApi,
      openBrowser: (url) async =>
          'projectforge://auth?code=c&state=${Uri.parse(url).queryParameters['state']}',
    );
    late ProviderContainer container;
    container = ProviderContainer(
      overrides: [
        sessionStoreProvider.overrideWithValue(store),
        appLoginProvider.overrideWithValue(login),
        jsonCacheProvider.overrideWithValue(JsonCache(() async => dir)),
        sharedFilesProvider.overrideWithValue(SharedFiles(() async => dir)),
        webCookiesProvider.overrideWithValue(cookies),
        deviceDescriptionProvider.overrideWith(
          (_) async => const DeviceDescription(
            name: 'iPhone',
            platform: 'ios',
            appVersion: '1',
          ),
        ),
        apiClientProvider.overrideWith(
          (ref) => ApiClient(
            serverUrl: serverUrl,
            token: () => container.read(authControllerProvider).session?.token,
            appVersion: '1',
            onUnauthorized: () {},
            onReauthenticate: () async => false,
            dio: Dio()..httpClientAdapter = api,
          ),
        ),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  Future<AuthController> loggedIn(ProviderContainer container) async {
    container.read(authControllerProvider);
    await Future<void>.delayed(Duration.zero);
    expect(container.read(authControllerProvider).session?.userId, 1);
    await JsonCache(() async => dir).write('businessCards', {'of': 'A'});
    await File(
      '${(await SharedFiles(() async => dir).directory()).path}/chat.pdf',
    ).writeAsString('A');
    return container.read(authControllerProvider.notifier);
  }

  setUp(() async {
    dir = await Directory.systemTemp.createTemp('pf_auth_test');
    store = _MemorySessionStore(userA);
    cookies = _FakeWebCookies();
    api = FakeAdapter((_) => (status: 200, body: null));
  });

  tearDown(() => dir.delete(recursive: true));

  test('a renewal of the same app keeps the data', () async {
    final c = container(userId: 1, deviceId: 10);
    final controller = await loggedIn(c);
    expect(await controller.reauthenticate(), isTrue);
    expect(c.read(authControllerProvider).session?.token, 'new');
    expect(await JsonCache(() async => dir).read('businessCards'), isNotNull);
    expect(cookies.cleared, 0);
    expect(api.requests, isEmpty);
  });

  test(
    'a renewal as another user logs the old one out and forgets their data',
    () async {
      final c = container(userId: 2, deviceId: 11);
      final controller = await loggedIn(c);
      expect(await controller.reauthenticate(), isFalse);
      final request = api.requests.single;
      expect(request.path, '/rs/appAuth/logout');
      expect(request.headers['Authorization'], 'PFApp tokenA');
      final state = c.read(authControllerProvider);
      expect(state.status, AuthStatus.loggedIn);
      expect(state.session?.userId, 2);
      expect(store.session?.userId, 2);
      expect(await JsonCache(() async => dir).read('businessCards'), isNull);
      expect(Directory('${dir.path}/shared').existsSync(), isFalse);
      expect(cookies.cleared, 1);
    },
  );

  for (final stored in [null, userA]) {
    test(
      'the start needs no server (${stored == null ? 'logged out' : 'logged in'})',
      () async {
        store.session = stored;
        final c = container(userId: 1, deviceId: 10);
        c.read(authControllerProvider);
        await Future<void>.delayed(Duration.zero);
        expect(
          c.read(authControllerProvider).status,
          stored == null ? AuthStatus.loggedOut : AuthStatus.loggedIn,
        );
        expect(api.requests, isEmpty);
        expect(loginApi.requests, isEmpty);
      },
    );
  }

  test('the logout reaches the login although the server fails', () async {
    api = FakeAdapter((_) => throw StateError('server down'));
    final c = container(userId: 1, deviceId: 10);
    final controller = await loggedIn(c);
    await controller.logout();
    expect(c.read(authControllerProvider).status, AuthStatus.loggedOut);
    expect(store.session, isNull);
  });

  group('proxy cookies of the WebView', () {
    const withProxy = Session(
      serverUrl: serverUrl,
      token: 'tokenA',
      userId: 1,
      deviceId: 10,
      proxyCookies: {'authentik_proxy_1': 'expired'},
    );

    test(
      'a renewed proxy cookie goes into the session, other cookies not',
      () async {
        store.session = withProxy;
        final c = container(userId: 1, deviceId: 10);
        final controller = await loggedIn(c);
        final renewed = await controller.takeProxyCookies([
          (name: 'JSESSIONID', value: 'web', domain: null),
          (name: 'authentik_proxy_1', value: 'expired', domain: null),
          (name: 'authentik_proxy_1', value: 'fresh', domain: null),
        ]);
        expect(renewed, isTrue);
        expect(c.read(authControllerProvider).session?.proxyCookies, {
          'authentik_proxy_1': 'fresh',
        });
        expect(store.session?.proxyCookies, {'authentik_proxy_1': 'fresh'});
        expect(store.session?.token, 'tokenA');
      },
    );

    test(
      'a cookie of the same name for the whole domain is not taken',
      () async {
        store.session = withProxy;
        final c = container(userId: 1, deviceId: 10);
        final controller = await loggedIn(c);
        expect(
          await controller.takeProxyCookies([
            (
              name: 'authentik_proxy_1',
              value: 'planted',
              domain: '.example.com',
            ),
          ]),
          isFalse,
        );
        expect(
          await controller.takeProxyCookies([
            (
              name: 'authentik_proxy_1',
              value: 'planted',
              domain: '.example.com',
            ),
            (
              name: 'authentik_proxy_1',
              value: 'fresh',
              domain: 'pf.example.com',
            ),
          ]),
          isTrue,
        );
        expect(store.session?.proxyCookies, {'authentik_proxy_1': 'fresh'});
      },
    );

    test(
      'two fresh values without their domains (Android) are not taken',
      () async {
        store.session = withProxy;
        final c = container(userId: 1, deviceId: 10);
        final controller = await loggedIn(c);
        expect(
          await controller.takeProxyCookies([
            (name: 'authentik_proxy_1', value: 'planted', domain: null),
            (name: 'authentik_proxy_1', value: 'fresh', domain: null),
          ]),
          isFalse,
        );
        expect(store.session?.proxyCookies, {'authentik_proxy_1': 'expired'});
      },
    );

    test('unchanged or missing cookies keep the session', () async {
      store.session = withProxy;
      final c = container(userId: 1, deviceId: 10);
      final controller = await loggedIn(c);
      expect(
        await controller.takeProxyCookies([
          (name: 'authentik_proxy_1', value: 'expired', domain: null),
        ]),
        isFalse,
      );
      expect(await controller.takeProxyCookies(const []), isFalse);
      expect(store.session?.proxyCookies, {'authentik_proxy_1': 'expired'});
    });

    test('without proxy cookies at the login nothing is taken', () async {
      final c = container(userId: 1, deviceId: 10);
      final controller = await loggedIn(c);
      expect(
        await controller.takeProxyCookies([
          (name: 'authentik_proxy_1', value: 'x', domain: null),
        ]),
        isFalse,
      );
      expect(store.session?.proxyCookies, isEmpty);
    });
  });

  for (final (userId, cleared) in [(2, true), (1, false)]) {
    test(
      'the log of an expired login is ${cleared ? 'cleared at the login of another user' : 'kept for the same user'}',
      () async {
        final c = container(userId: userId, deviceId: 10);
        final controller = await loggedIn(c);
        appLog.info('web', 'Navigation to a page of the user');
        controller.onUnauthorized();
        // The local data is deleted first.
        for (var i = 0; i < 100; i++) {
          if (c.read(authControllerProvider).status == AuthStatus.loggedOut) {
            break;
          }
          await Future<void>.delayed(const Duration(milliseconds: 10));
        }
        expect(c.read(authControllerProvider).status, AuthStatus.loggedOut);
        await controller.login(serverUrl);
        expect(c.read(authControllerProvider).session?.userId, userId);
        expect(
          appLog.entries.map((it) => it.message),
          cleared
              ? isNot(contains('Navigation to a page of the user'))
              : contains('Navigation to a page of the user'),
        );
      },
    );
  }

  test('the logout clears the log of the login', () async {
    final c = container(userId: 1, deviceId: 10);
    final controller = await loggedIn(c);
    appLog.info('web', 'Navigation to a page of the user');
    await controller.logout();
    expect(appLog.entries.map((it) => it.message), [
      'Logged out: log of the login cleared',
    ]);
  });
}
