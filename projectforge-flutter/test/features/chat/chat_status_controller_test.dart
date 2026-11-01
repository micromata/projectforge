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
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/api/api_client.dart';
import 'package:projectforge_app/features/chat/chat_status.dart';
import 'package:projectforge_app/features/chat/chat_status_controller.dart';
import 'package:projectforge_app/features/chat/chat_status_repository.dart';

import '../../core/fake_adapter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('unread count and switched off chat', () {
    expect(ChatStatus({'chatUnread': 3}).unread, 3);
    expect(ChatStatus({}).unread, 0);
    expect(ChatStatus({}).enabled, isTrue);
    expect(
      ChatStatus({
        'disabledFeatures': ['CALENDAR'],
      }).enabled,
      isTrue,
    );
    expect(
      ChatStatus({
        'disabledFeatures': ['CHAT'],
      }).enabled,
      isFalse,
    );
  });

  test('offline the last status stays', () async {
    var offline = false;
    final adapter = FakeAdapter((options) {
      if (offline) {
        throw DioException.connectionError(
          requestOptions: options,
          reason: 'offline',
        );
      }
      return (status: 200, body: {'chatUnread': 2});
    });
    final container = ProviderContainer(
      overrides: [
        chatStatusRepositoryProvider.overrideWithValue(
          ChatStatusRepository(
            ApiClient(
              serverUrl: 'https://pf.example.com',
              token: () => 'token',
              appVersion: '1',
              onUnauthorized: () {},
              onReauthenticate: () async => false,
              dio: Dio()..httpClientAdapter = adapter,
            ),
          ),
        ),
      ],
    );
    addTearDown(container.dispose);

    expect((await container.read(chatStatusProvider.future)).unread, 2);
    expect(adapter.requests.single.path, '/rs/userStatus');
    offline = true;
    await container.read(chatStatusProvider.notifier).refresh();
    expect(container.read(chatStatusProvider).value?.unread, 2);
  });
}
