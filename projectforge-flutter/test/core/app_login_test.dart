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
import 'package:projectforge_app/core/api/api_exception.dart';
import 'package:projectforge_app/core/auth/app_login.dart';
import 'package:projectforge_app/core/auth/pkce.dart';

import 'fake_adapter.dart';

void main() {
  test('exchanges the code of the callback with the verifier of the challenge', () async {
    late Uri opened;
    final adapter = FakeAdapter(
      (options) => (
        status: 200,
        body: {
          'userId': 7,
          'username': 'kai',
          'fullname': 'Kai R',
          'deviceId': 3,
          'token': 'secret',
        },
      ),
    );
    final login = AppLogin(
      dio: Dio()..httpClientAdapter = adapter,
      openBrowser: (url) async {
        opened = Uri.parse(url);
        return 'projectforge://auth?code=theCode&state=${opened.queryParameters['state']}';
      },
    );
    final session = await login.login(
      serverUrl: 'https://pf.example.com',
      deviceName: 'iPhone',
      platform: 'ios',
    );
    expect(opened.path, '/next/appLogin');
    expect(opened.queryParameters['device'], 'iPhone');
    final body = adapter.requests.single.data as Map<String, dynamic>;
    expect(
      adapter.requests.single.uri.toString(),
      'https://pf.example.com/rsPublic/appAuth/token',
    );
    expect(body['code'], 'theCode');
    expect(
      Pkce.challengeOf(body['codeVerifier'] as String),
      opened.queryParameters['challenge'],
    );
    expect(session.token, 'secret');
    expect(session.deviceId, 3);
  });

  test('refuses a callback with another state', () async {
    final login = AppLogin(
      dio: Dio()
        ..httpClientAdapter = FakeAdapter((_) => (status: 200, body: {})),
      openBrowser: (_) async => 'projectforge://auth?code=x&state=foreign',
    );
    expect(
      login.login(
        serverUrl: 'https://pf.example.com',
        deviceName: 'd',
        platform: 'ios',
      ),
      throwsA(isA<ApiException>()),
    );
  });

  test('a declined login is a cancellation', () async {
    late String state;
    final login = AppLogin(
      dio: Dio()
        ..httpClientAdapter = FakeAdapter((_) => (status: 200, body: {})),
      openBrowser: (url) async {
        state = Uri.parse(url).queryParameters['state']!;
        return 'projectforge://auth?error=access_denied&state=$state';
      },
    );
    expect(
      login.login(
        serverUrl: 'https://pf.example.com',
        deviceName: 'd',
        platform: 'ios',
      ),
      throwsA(isA<AppLoginCancelled>()),
    );
  });
}
