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
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/api/api_client.dart';
import 'package:projectforge_app/core/i18n/translations.dart';
import 'package:projectforge_app/core/status/user_status_controller.dart';
import 'package:projectforge_app/core/status/user_status_repository.dart';
import 'package:projectforge_app/features/home/system_alert_banner.dart';

import '../../core/fake_adapter.dart';

Widget _app(Map<String, dynamic> status) {
  final adapter = FakeAdapter((options) => (status: 200, body: status));
  return ProviderScope(
    overrides: [
      userStatusRepositoryProvider.overrideWithValue(
        UserStatusRepository(
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
    child: TranslationsScope(
      translations: Translations('en', {
        'app.systemMessage.title': 'System message',
      }),
      child: const MaterialApp(home: Scaffold(body: SystemAlertBanner())),
    ),
  );
}

void main() {
  testWidgets('shows the message of the administrators', (tester) async {
    await tester.pumpWidget(_app({'alertMessage': 'Maintenance at 8 pm'}));
    await tester.pumpAndSettle();
    expect(find.text('Maintenance at 8 pm'), findsOneWidget);
  });

  testWidgets('shows nothing without a message', (tester) async {
    await tester.pumpWidget(_app({'alertMessage': '  '}));
    await tester.pumpAndSettle();
    expect(find.byType(Text), findsNothing);
  });
}
