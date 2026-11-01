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

import 'fake_adapter.dart';

void main() {
  late String token;
  late int unauthorized;
  late int reauthentications;

  ApiClient client(FakeAdapter adapter, {bool reauthSucceeds = true}) =>
      ApiClient(
        serverUrl: 'https://pf.example.com',
        token: () => token,
        appVersion: '1.0.0+1',
        onUnauthorized: () => unauthorized++,
        onReauthenticate: () async {
          reauthentications++;
          if (reauthSucceeds) token = 'renewed';
          return reauthSucceeds;
        },
        dio: Dio()..httpClientAdapter = adapter,
      );

  setUp(() {
    token = 'first';
    unauthorized = 0;
    reauthentications = 0;
  });

  test('sends the app token and the client headers', () async {
    final adapter = FakeAdapter((_) => (status: 200, body: {'ok': true}));
    await client(adapter).get<Map<String, dynamic>>('/rs/myBusinessCards');
    final headers = adapter.requests.single.headers;
    expect(headers['Authorization'], 'PFApp first');
    expect(headers['X-PF-Frontend'], 'next');
    expect(headers['X-PF-App-Version'], '1.0.0+1');
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
}
