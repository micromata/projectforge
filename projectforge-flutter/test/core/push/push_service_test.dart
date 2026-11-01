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
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/api/api_client.dart';
import 'package:projectforge_app/core/push/push_service.dart';

import '../fake_adapter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('test/push');

  late FakeAdapter adapter;
  late List<String> links;
  var received = 0;

  PushService service({bool production = false, bool offline = false}) {
    links = [];
    received = 0;
    adapter = FakeAdapter((options) {
      if (offline) {
        throw DioException.connectionError(
          requestOptions: options,
          reason: 'offline',
        );
      }
      return (status: 200, body: null);
    });
    final api = ApiClient(
      serverUrl: 'https://pf.example.com',
      token: () => 't',
      appVersion: '1',
      onUnauthorized: () {},
      onReauthenticate: () async => false,
      dio: Dio()..httpClientAdapter = adapter,
    );
    return PushService(
      api,
      onLink: links.add,
      onReceived: () => received++,
      channel: channel,
      production: production,
    );
  }

  void native(Map<String, dynamic>? startAnswer) {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
          channel,
          (call) async => call.method == 'start' ? startAnswer : null,
        );
  }

  Future<void> fromNative(String method, Object? arguments) async {
    await TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .handlePlatformMessage(
          channel.name,
          channel.codec.encodeMethodCall(MethodCall(method, arguments)),
          (_) {},
        );
  }

  test('token goes to the server once, with the environment', () async {
    native({'granted': true});
    final push = service();
    await push.start();
    await fromNative('token', 'ab12');
    await fromNative('token', 'ab12');
    expect(adapter.requests, hasLength(1));
    expect(adapter.requests.single.path, '/rs/appAuth/pushToken');
    expect(adapter.requests.single.data, {
      'token': 'ab12',
      'environment': 'SANDBOX',
    });
  });

  test('release builds use production', () async {
    native({'granted': true});
    final push = service(production: true);
    await push.start();
    await fromNative('token', 'ab12');
    expect(adapter.requests.single.data['environment'], 'PRODUCTION');
  });

  test('offline the token is sent again next time', () async {
    native({'granted': true});
    final push = service(offline: true);
    await push.start();
    await fromNative('token', 'ab12');
    await fromNative('token', 'ab12');
    expect(adapter.requests, hasLength(2));
  });

  test('links of taps and of the start', () async {
    native({'granted': true, 'launchLink': '/chat/5'});
    final push = service();
    await push.start();
    expect(links, ['/chat/5']);
    await fromNative('link', '/timesheet');
    await fromNative('received', null);
    expect(links, ['/chat/5', '/timesheet']);
    expect(received, 1);
  });
}
