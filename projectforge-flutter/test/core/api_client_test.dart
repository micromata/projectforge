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

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/api/api_client.dart';
import 'package:projectforge_app/core/api/api_exception.dart';
import 'package:projectforge_app/core/log/app_log.dart';

import 'fake_adapter.dart';

void main() {
  late String token;
  late int unauthorized;
  late int reauthentications;
  late Map<String, String> proxyCookies;

  ApiClient client(FakeAdapter adapter, {bool reauthSucceeds = true}) =>
      ApiClient(
        serverUrl: 'https://pf.example.com',
        token: () => token,
        appVersion: '1.0.0+1',
        onUnauthorized: () => unauthorized++,
        onReauthenticate: () async {
          reauthentications++;
          if (reauthSucceeds) {
            token = 'renewed';
            proxyCookies = {'authentik_proxy_1': 'fresh'};
          }
          return reauthSucceeds;
        },
        proxyCookies: () => proxyCookies,
        dio: Dio()..httpClientAdapter = adapter,
      );

  setUp(() {
    token = 'first';
    unauthorized = 0;
    reauthentications = 0;
    proxyCookies = const {};
    appLog.clear();
  });

  /// An SSO proxy (Authentik) lets only requests with its fresh cookie through, otherwise it answers itself.
  FakeAdapter proxy({
    required int status,
    Object body = const RawBody('<html></html>'),
  }) => FakeAdapter(
    (options) => options.headers['Cookie'] == 'authentik_proxy_1=fresh'
        ? (status: 200, body: {'ok': true})
        : (status: status, body: body),
  );

  test('sends the app token and the client headers', () async {
    final adapter = FakeAdapter((_) => (status: 200, body: {'ok': true}));
    await client(adapter).get<Map<String, dynamic>>('/rs/myBusinessCards');
    final headers = adapter.requests.single.headers;
    expect(headers['Authorization'], 'PFApp first');
    expect(headers['X-PF-Frontend'], 'next');
    expect(headers['X-PF-App-Version'], '1.0.0+1');
    expect(headers.containsKey('Cookie'), isFalse, reason: 'No proxy.');
  });

  test('sends the cookies of an SSO proxy', () async {
    proxyCookies = {'authentik_proxy_1': 'a', 'other': 'b'};
    final adapter = FakeAdapter((_) => (status: 200, body: {'ok': true}));
    await client(adapter).get<Object>('/rs/x');
    expect(
      adapter.requests.single.headers['Cookie'],
      'authentik_proxy_1=a; other=b',
    );
  });

  test(
    'a refusal of the proxy renews the login instead of logging out',
    () async {
      for (final status in [401, 302]) {
        reauthentications = 0;
        proxyCookies = {'authentik_proxy_1': 'expired'};
        final adapter = proxy(status: status);
        final result = await client(adapter).get<Map<String, dynamic>>('/rs/x');
        expect(result['ok'], true, reason: '$status');
        expect(reauthentications, 1);
        expect(adapter.requests.length, 2);
      }
      expect(unauthorized, 0);
    },
  );

  test('a cancelled renewal fails with the text of the proxy', () async {
    await expectLater(
      client(proxy(status: 401), reauthSucceeds: false).get<Object>('/rs/x'),
      throwsA(
        isA<ApiException>().having(
          (e) => e.messageKey,
          'messageKey',
          ApiClient.proxyRefusedKey,
        ),
      ),
    );
    expect(unauthorized, 0);
  });

  test(
    'a renewal the proxy doesn\'t accept either isn\'t tried again',
    () async {
      final adapter = FakeAdapter(
        (_) => (status: 401, body: const RawBody('<html></html>')),
      );
      final api = client(adapter);
      for (var i = 0; i < 2; i++) {
        await expectLater(
          api.get<Object>('/rs/x'),
          throwsA(isA<ApiException>()),
        );
      }
      expect(reauthentications, 1);
      expect(adapter.requests.length, 3);
      expect(unauthorized, 0);
    },
  );

  test('the logout never renews the login', () async {
    await expectLater(
      client(proxy(status: 401))
          .post<void>('/rs/appAuth/logout', renewLogin: false),
      throwsA(isA<ApiException>()),
    );
    expect(reauthentications, 0);
  });

  test('polling in the background never renews the login', () async {
    await expectLater(
      client(proxy(status: 401))
          .get<Object>('/rs/userStatus', renewLogin: false),
      throwsA(
        isA<ApiException>().having(
          (e) => e.messageKey,
          'messageKey',
          ApiClient.proxyRefusedKey,
        ),
      ),
    );
    expect(reauthentications, 0);
    expect(unauthorized, 0);
  });

  test(
    'proxyRefuses tells a refusal of the proxy, never renewing the login',
    () async {
      for (final status in [401, 302]) {
        proxyCookies = {'authentik_proxy_1': 'expired'};
        expect(
          await client(proxy(status: status)).proxyRefuses(),
          isTrue,
          reason: '$status',
        );
        proxyCookies = {'authentik_proxy_1': 'fresh'};
        expect(
          await client(proxy(status: status)).proxyRefuses(),
          isFalse,
          reason: 'The proxy lets the app through ($status).',
        );
      }
      expect(
        await client(FakeAdapter((_) => (status: 500, body: {'message': 'x'})))
            .proxyRefuses(),
        isFalse,
        reason: 'An error of the server is no refusal of the proxy.',
      );
      expect(reauthentications, 0);
    },
  );

  test('a 401 of the server logs the app out, also for a download', () async {
    final adapter = FakeAdapter((_) => (status: 401, body: {'status': 401}));
    await expectLater(
      client(adapter).download('/rs/vcard'),
      throwsA(isA<ApiException>()),
    );
    expect(unauthorized, 1);
    expect(reauthentications, 0);
  });

  test('401 logs the app out', () async {
    final adapter = FakeAdapter((_) => (status: 401, body: {'status': 401}));
    await expectLater(
      client(adapter).get<Object>('/rs/x'),
      throwsA(isA<ApiException>()),
    );
    expect(unauthorized, 1);
  });

  test(
    'a required second factor renews the login and repeats the request once',
    () async {
      final adapter = FakeAdapter(
        (options) => options.headers['Authorization'] == 'PFApp renewed'
            ? (status: 200, body: {'ok': true})
            : (
                status: 403,
                body: {'twoFactorRequired': true, 'expiryMillis': 600000},
              ),
      );
      final result = await client(adapter)
          .get<Map<String, dynamic>>('/rs/order/list');
      expect(result['ok'], true);
      expect(reauthentications, 1);
      expect(adapter.requests.length, 2);
    },
  );

  test('a cancelled renewal fails the request', () async {
    final adapter = FakeAdapter(
      (_) => (status: 403, body: {'twoFactorRequired': true}),
    );
    await expectLater(
      client(adapter, reauthSucceeds: false).get<Object>('/rs/order/list'),
      throwsA(isA<ApiException>().having((e) => e.statusCode, 'status', 403)),
    );
    expect(adapter.requests.length, 1);
  });

  test('406 carries the validation errors', () async {
    final adapter = FakeAdapter(
      (_) => (
        status: 406,
        body: {
          'validationErrors': [
            {
              'message': 'Please name the card.',
              'fieldId': 'cardName',
              'messageId': 'businessCard.error.nameRequired',
            },
          ],
        },
      ),
    );
    final error = await client(adapter)
        .post<Object>('/rs/myBusinessCards/save')
        .then<ApiException?>(
          (_) => null,
          onError: (Object e) => e as ApiException,
        );
    expect(error!.statusCode, 406);
    expect(error.validationErrors.single.fieldId, 'cardName');
    expect(error.message, 'Please name the card.');
  });

  test('an unreachable server is offline', () async {
    final dio = Dio()
      ..httpClientAdapter = FakeAdapter(
        (_) => throw DioException.connectionError(
          requestOptions: RequestOptions(),
          reason: 'no network',
        ),
      );
    final api = ApiClient(
      serverUrl: 'https://pf.example.com',
      token: () => token,
      appVersion: '',
      onUnauthorized: () {},
      onReauthenticate: () async => false,
      dio: dio,
    );
    await expectLater(
      api.get<Object>('/rs/x'),
      throwsA(isA<ApiException>().having((e) => e.isOffline, 'offline', true)),
    );
  });

  test('a refusal of the proxy is logged without token, cookie values or query values', () async {
    proxyCookies = {'authentik_proxy_1': 'expired'};
    final adapter = proxy(status: 401);
    await client(adapter)
        .get<Object>('/rs/x', query: {'code': 'c0de', 'q': 'secret query'});
    final log = appLog.text();
    expect(
      log,
      contains('GET https://pf.example.com/rs/x?code=***&q=*** → 401'),
    );
    expect(log, contains('content-type: text/html'));
    expect(log, contains('proxy cookies sent: authentik_proxy_1'));
    expect(log, contains('refused by the SSO proxy'));
    expect(log, contains('(retry)'));
    for (final secret in [
      'expired',
      'fresh',
      'first',
      'renewed',
      'c0de',
      'secret query',
    ]) {
      expect(log, isNot(contains(secret)), reason: secret);
    }
  });
}
