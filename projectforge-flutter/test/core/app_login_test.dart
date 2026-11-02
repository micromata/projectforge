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

import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/api/api_exception.dart';
import 'package:projectforge_app/core/auth/app_login.dart';
import 'package:projectforge_app/core/auth/pkce.dart';
import 'package:projectforge_app/core/auth/session.dart';

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
          'proxyCookies': {'authentik_proxy_1': 'proxySession'},
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
    expect(session.proxyCookies, {'authentik_proxy_1': 'proxySession'});
    expect(
      Session.fromJson(session.toJson()).proxyCookies,
      session.proxyCookies,
      reason: 'Kept in the key chain with the token.',
    );
  });

  test('the parameters go packed first, so an SSO proxy keeping only the first one passes them on', () async {
    late String opened;
    final login = AppLogin(
      dio: Dio()
        ..httpClientAdapter = FakeAdapter(
          (_) =>
              (status: 200, body: {'userId': 7, 'deviceId': 3, 'token': 't'}),
        ),
      openBrowser: (url) async {
        opened = url;
        return 'projectforge://auth?code=c&state=${Uri.parse(url).queryParameters['state']}';
      },
    );
    await login.login(
      serverUrl: 'https://pf.example.com',
      deviceName: 'Jörgs iPhone',
      platform: 'ios',
      deviceId: 3,
    );
    final query = Uri.parse(opened).query;
    expect(query, startsWith('login='));
    // What an unencoded rd=…?login=…&challenge=… of the proxy leaves over.
    final kept = Uri.splitQueryString(query.split('&').first)['login']!;
    final packed = jsonDecode(
      utf8.decode(base64Url.decode(base64Url.normalize(kept))),
    ) as Map<String, dynamic>;
    final single = Uri.parse(opened).queryParameters;
    expect(packed, {
      'challenge': single['challenge'],
      'state': single['state'],
      'device': 'Jörgs iPhone',
      'platform': 'ios',
      'deviceId': 3,
    });
    expect(kept, isNot(contains('=')), reason: 'No padding.');
  });

  test('a stored session of an older version has no proxy cookies', () {
    final session = Session.fromJson({
      'serverUrl': 'https://pf.example.com',
      'token': 'secret',
      'userId': 7,
      'deviceId': 3,
    });
    expect(session.proxyCookies, isEmpty);
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

  test('the parameters go into the path too, for a server taking them there', () async {
    late Uri opened;
    final login = AppLogin(
      dio: Dio()
        ..httpClientAdapter = FakeAdapter(
          (_) => (
            status: 200,
            body: {
              'userId': 7,
              'deviceId': 3,
              'token': 't',
              'appLoginPath': true,
            },
          ),
        ),
      openBrowser: (url) async {
        opened = Uri.parse(url);
        return 'projectforge://auth?code=c&state=${opened.queryParameters['state']}';
      },
    );
    final session = await login.login(
      serverUrl: 'https://pf.example.com',
      deviceName: 'iPhone',
      platform: 'ios',
      loginPath: true,
    );
    // What a proxy bringing the browser back without the query leaves over.
    expect(opened.pathSegments, [
      'next',
      'appLogin',
      opened.queryParameters['login'],
    ]);
    expect(session.appLoginPath, isTrue);
    expect(
      Session.fromJson(session.toJson()).appLoginPath,
      isTrue,
      reason: 'Kept for the next login.',
    );
  });
}
