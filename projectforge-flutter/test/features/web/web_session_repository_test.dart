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
import 'package:projectforge_app/core/web/web_cookies.dart';
import 'package:projectforge_app/features/web/web_session_repository.dart';

import '../../core/fake_adapter.dart';

/// No WebView in tests: the cookies are only counted.
class _FakeCookies extends WebCookies {
  int calls = 0;

  @override
  Future<void> setCookies(String serverUrl, List<String> headers) async =>
      calls++;
}

void main() {
  late FakeAdapter adapter;
  late WebSessionRepository repository;
  var status = 200;

  setUp(() {
    status = 200;
    adapter = FakeAdapter(
      (_) => (status: status, body: {'startUrl': '/next/start'}),
    );
    repository = WebSessionRepository(
      ApiClient(
        serverUrl: 'https://pf.example.com',
        token: () => 'token',
        appVersion: '1',
        onUnauthorized: () {},
        onReauthenticate: () async => false,
        dio: Dio()..httpClientAdapter = adapter,
      ),
      _FakeCookies(),
    );
  });

  test('all tabs share one web session', () async {
    expect(await repository.ensure(), '/next/start');
    expect(await repository.ensure(), '/next/start');
    expect(adapter.requests, hasLength(1));
    expect(adapter.requests.single.path, '/rs/appAuth/webSession');
  });

  test(
    'concurrent renewals open one new session, used by later tabs',
    () async {
      await repository.ensure();
      await Future.wait([repository.renew(), repository.renew()]);
      expect(adapter.requests, hasLength(2));
      await repository.ensure();
      expect(adapter.requests, hasLength(2));
    },
  );

  test('a failed attempt is tried again', () async {
    status = 500;
    await expectLater(repository.ensure(), throwsA(isA<ApiException>()));
    status = 200;
    expect(await repository.ensure(), '/next/start');
    expect(adapter.requests, hasLength(2));
  });
}
