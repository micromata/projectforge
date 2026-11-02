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

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:projectforge_app/core/api/api_client.dart';
import 'package:projectforge_app/core/cache/json_cache.dart';
import 'package:projectforge_app/core/i18n/translations.dart';
import 'package:projectforge_app/features/timesheets/timesheets_controller.dart';
import 'package:projectforge_app/features/timesheets/timesheets_repository.dart';
import 'package:projectforge_app/features/timesheets/timesheets_screen.dart';

import '../../core/fake_adapter.dart';

String _iso(int hour, [int minute = 0]) {
  final now = DateTime.now();
  return DateTime(
    now.year,
    now.month,
    now.day,
    hour,
    minute,
  ).toUtc().toIso8601String();
}

Map<String, dynamic> _sheet(int id, int from, int to, String description) => {
  'id': id,
  'task': {'id': 3, 'title': 'Development', 'path': 'ProjectForge -> Next'},
  'description': description,
  'startTime': _iso(from),
  'stopTime': _iso(to),
};

/// No file access: the files of the real cache don't complete in the fake time of a widget test.
class _MemoryCache extends JsonCache {
  final _files = <String, Map<String, dynamic>>{};

  @override
  Future<Map<String, dynamic>?> read(String name) async => _files[name];

  @override
  Future<void> write(String name, Map<String, dynamic> json) async =>
      _files[name] = json;

  @override
  Future<void> delete(String name) async => _files.remove(name);
}

void main() {
  late FakeAdapter api;
  late Translations translations;

  setUpAll(() async {
    TestWidgetsFlutterBinding.ensureInitialized();
    await initializeDateFormatting('en');
    translations = await Translations.load(const Locale('en'));
  });

  setUp(() async {
    api = FakeAdapter((o) {
      switch ((o.method, o.path)) {
        case ('GET', '/rs/timesheet/appList'):
          return (
            status: 200,
            body: {
              'timesheets': [
                _sheet(1, 8, 10, 'Morning work'),
                _sheet(2, 11, 12, 'Before lunch'),
              ],
              'tags': <String>[],
            },
          );
        case ('GET', '/rs/timesheet/recentList'):
          return (
            status: 200,
            body: {
              'timesheets': [
                {
                  'task': {
                    'id': 3,
                    'title': 'Development',
                    'path': 'ProjectForge -> Next',
                  },
                  'description': 'Recent work',
                },
              ],
            },
          );
        case ('GET', '/rs/timesheet/favorites/list'):
          return (status: 200, body: <Object>[]);
        case ('GET', '/rs/task/info/3'):
          return (
            status: 200,
            body: {'id': 3, 'title': 'Development', 'kost2List': <Object>[]},
          );
        case ('PUT', '/rs/timesheet/insert'):
          return (status: 200, body: {});
      }
      return (status: 404, body: {'message': '${o.method} ${o.path}'});
    });
  });

  Future<void> pumpScreen(WidgetTester tester) async {
    tester.view.physicalSize = const Size(390, 844) * 3;
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    final repository = TimesheetsRepository(
      ApiClient(
        serverUrl: 'https://pf.example.com',
        token: () => 't',
        appVersion: '1',
        onUnauthorized: () {},
        onReauthenticate: () async => false,
        dio: Dio()..httpClientAdapter = api,
      ),
      _MemoryCache(),
      1,
    );
    await tester.pumpWidget(
      ProviderScope(
        overrides: [timesheetsRepositoryProvider.overrideWithValue(repository)],
        child: TranslationsScope(
          translations: translations,
          child: const MaterialApp(
            home: Scaffold(body: TimesheetsScreen(active: true)),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets(
    'the day shows its bookings, the total and the gap between them',
    (tester) async {
      await pumpScreen(tester);
      expect(find.text('Morning work'), findsOneWidget);
      expect(find.text('Before lunch'), findsOneWidget);
      expect(find.text('Σ 3:00 h'), findsOneWidget);
      expect(find.text('10:00–11:00 · 1:00 h free · book'), findsOneWidget);
      expect(find.text('Next › Development'), findsNWidgets(2));
    },
  );

  testWidgets(
    'a tap on the gap books exactly that time, with a recent entry as the start',
    (tester) async {
      await pumpScreen(tester);
      await tester.tap(find.text('10:00–11:00 · 1:00 h free · book'));
      await tester.pumpAndSettle();
      expect(find.text('New time sheet'), findsOneWidget);
      expect(find.text('10:00'), findsWidgets);
      expect(find.text('11:00'), findsWidgets);

      await tester.tap(find.text('Recent work'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Book · 1:00 h'));
      await tester.pumpAndSettle();

      final insert = api.requests.singleWhere(
        (it) => it.path == '/rs/timesheet/insert',
      );
      final data =
          (insert.data as Map<String, dynamic>)['data'] as Map<String, dynamic>;
      expect(data['task'], {'id': 3});
      expect(data['description'], 'Recent work');
      expect(data['startTime'], _iso(10));
      expect(data['stopTime'], _iso(11));
      expect(find.text('New time sheet'), findsNothing);
    },
  );

  testWidgets('without a task nothing is sent', (tester) async {
    await pumpScreen(tester);
    await tester.tap(find.text('Book'));
    await tester.pumpAndSettle();
    await tester.tap(find.textContaining('Book ·'));
    await tester.pumpAndSettle();
    expect(find.text('Please choose a structure element.'), findsOneWidget);
    expect(api.requests.where((it) => it.method == 'PUT'), isEmpty);
  });

  testWidgets('the refusal of the server is shown at the time', (tester) async {
    final answers = api.handler;
    api = FakeAdapter((o) {
      if (o.method == 'PUT') {
        return (
          status: 406,
          body: {
            'validationErrors': [
              {'message': 'Overlap with 08:00-10:00.'},
            ],
          },
        );
      }
      return answers(o);
    });
    await pumpScreen(tester);
    await tester.tap(find.text('10:00–11:00 · 1:00 h free · book'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Recent work'));
    await tester.pumpAndSettle();
    await tester.tap(find.textContaining('Book ·'));
    await tester.pumpAndSettle();
    expect(find.text('Overlap with 08:00-10:00.'), findsOneWidget);
    expect(
      find.text('New time sheet'),
      findsOneWidget,
      reason: 'the form stays open',
    );
  });
}
