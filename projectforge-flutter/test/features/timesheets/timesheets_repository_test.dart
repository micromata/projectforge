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

import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/api/api_client.dart';
import 'package:projectforge_app/core/api/api_exception.dart';
import 'package:projectforge_app/core/cache/json_cache.dart';
import 'package:projectforge_app/features/timesheets/timesheet.dart';
import 'package:projectforge_app/features/timesheets/timesheets_repository.dart';

import '../../core/fake_adapter.dart';

Map<String, dynamic> _sheet(int id, int hour) => {
  'id': id,
  'task': {'id': 3, 'title': 'Dev', 'path': 'ProjectForge'},
  'description': 'Tests $id',
  'timeSavedByAI': 25,
  'startTime': DateTime(2031, 3, 10, hour).toUtc().toIso8601String(),
  'stopTime': DateTime(2031, 3, 10, hour + 1).toUtc().toIso8601String(),
};

void main() {
  late Directory dir;
  late FakeAdapter api;
  late TimesheetsRepository repository;
  var online = true;
  ({int status, Object? body}) Function(RequestOptions options) answer = (_) =>
      (status: 200, body: null);

  setUp(() async {
    dir = await Directory.systemTemp.createTemp('pf_timesheets_test');
    online = true;
    api = FakeAdapter(
      (o) => online
          ? answer(o)
          : throw DioException(
              requestOptions: o,
              type: DioExceptionType.connectionError,
            ),
    );
    repository = TimesheetsRepository(
      ApiClient(
        serverUrl: 'https://pf.example.com',
        token: () => 't',
        appVersion: '1',
        onUnauthorized: () {},
        onReauthenticate: () async => false,
        dio: Dio()..httpClientAdapter = api,
      ),
      JsonCache(() async => dir),
      42,
    );
  });

  tearDown(() => dir.delete(recursive: true));

  final monday = DateTime(2031, 3, 10);

  test(
    'a week is asked for by its period and kept for reading offline',
    () async {
      answer = (_) => (
        status: 200,
        body: {
          'timesheets': [_sheet(1, 8)],
          'tags': ['Remote'],
        },
      );
      final week = await repository.week(monday);
      expect(week.sheets.single.id, 1);
      expect(week.fromCache, isFalse);
      final request = api.requests.single;
      expect(request.path, '/rs/timesheet/appList');
      expect(request.queryParameters['from'], monday.millisecondsSinceEpoch);
      expect(
        request.queryParameters['to'],
        DateTime(2031, 3, 17).millisecondsSinceEpoch,
      );

      online = false;
      final cached = await repository.week(monday);
      expect(cached.fromCache, isTrue);
      expect(cached.sheets.single.what.description, 'Tests 1');
      expect(cached.tags, ['Remote']);
      // A week never seen has nothing to show.
      await expectLater(
        repository.week(DateTime(2031, 3, 17)),
        throwsA(isA<ApiException>()),
      );
    },
  );

  test(
    'a new booking is inserted for the user, the times as UTC instants',
    () async {
      final draft = BookingDraft(
        task: const TaskRef(id: 3, title: 'Dev'),
        kost2: const Kost2Ref(id: 7, label: '5.020.01.02'),
        start: DateTime(2031, 3, 10, 8),
        stop: DateTime(2031, 3, 10, 10, 30),
        description: ' Tests ',
        tag: 'Remote',
      );
      await repository.save(draft);
      final request = api.requests.single;
      expect(request.method, 'PUT');
      expect(request.path, '/rs/timesheet/insert');
      final data =
          (request.data as Map<String, dynamic>)['data']
              as Map<String, dynamic>;
      expect(data['task'], {'id': 3});
      expect(data['kost2'], {'id': 7});
      expect(data['user'], {'id': 42});
      expect(
        data['startTime'],
        DateTime(2031, 3, 10, 8).toUtc().toIso8601String(),
      );
      expect(
        data['stopTime'],
        DateTime(2031, 3, 10, 10, 30).toUtc().toIso8601String(),
      );
      expect(data['tag'], 'Remote');
      expect(data.containsKey('id'), isFalse);
    },
  );

  test(
    'an edit starts from the whole sheet, so unknown fields survive',
    () async {
      answer = (o) => o.method == 'GET'
          ? (status: 200, body: _sheet(5, 8))
          : (status: 200, body: {});
      await repository.save(
        BookingDraft(
          task: const TaskRef(id: 3, title: 'Dev'),
          start: DateTime(2031, 3, 10, 8),
          stop: DateTime(2031, 3, 10, 9, 30),
          description: 'Changed',
        ),
        id: 5,
      );
      expect(api.requests.map((it) => '${it.method} ${it.path}'), [
        'GET /rs/timesheet/5',
        'PUT /rs/timesheet/update',
      ]);
      final data =
          (api.requests.last.data as Map<String, dynamic>)['data']
              as Map<String, dynamic>;
      expect(data['id'], 5);
      expect(data['timeSavedByAI'], 25);
      expect(data['description'], 'Changed');
      expect(data['kost2'], isNull);
      expect(
        data.containsKey('user'),
        isFalse,
        reason: 'the owner stays the one of the stored sheet',
      );
    },
  );

  test('the refusals of the server come with their fields', () async {
    answer = (_) => (
      status: 406,
      body: {
        'validationErrors': [
          {'fieldId': 'kost2', 'message': 'Please choose cost 2 id.'},
          {'message': 'Overlap with 08:00-09:00.'},
        ],
      },
    );
    try {
      await repository.save(
        BookingDraft(
          task: const TaskRef(id: 3, title: 'Dev'),
          start: DateTime(2031, 3, 10, 8),
          stop: DateTime(2031, 3, 10, 9),
        ),
      );
      fail('refused');
    } on ApiException catch (e) {
      expect(e.statusCode, 406);
      expect(e.validationErrors.map((it) => it.fieldId), ['kost2', null]);
    }
  });

  test('a deleted booking can be restored with what it was', () async {
    answer = (o) => o.method == 'GET'
        ? (status: 200, body: _sheet(5, 8))
        : (status: 200, body: {});
    final data = await repository.delete(5);
    expect(api.requests.last.method, 'DELETE');
    expect(api.requests.last.path, '/rs/timesheet/markAsDeleted');
    await repository.undelete(data);
    expect(api.requests.last.method, 'PUT');
    expect(api.requests.last.path, '/rs/timesheet/undelete');
    expect(((api.requests.last.data as Map)['data'] as Map)['id'], 5);
  });

  test('recent bookings are listed once, a template resolves its task and cost unit', () async {
    answer = (o) {
      if (o.path == '/rs/timesheet/recentList') {
        return (
          status: 200,
          body: {
            'timesheets': [
              {
                'task': {'id': 3, 'title': 'Dev'},
                'description': 'A',
              },
              {
                'task': {'id': 3, 'title': 'Dev'},
                'description': 'A',
              },
              {
                'task': {'id': 4, 'title': 'Review'},
                'description': 'B',
              },
              {'description': 'no task'},
            ],
          },
        );
      }
      return (
        status: 200,
        body: {
          'id': 3,
          'title': 'Dev',
          'path': [
            {'id': 1, 'title': 'ProjectForge'},
          ],
          'kost2List': [
            {'id': 6, 'displayName': 'first'},
            {'id': 7, 'displayName': 'second'},
          ],
        },
      );
    };
    expect((await repository.recent()).map((it) => it.description), ['A', 'B']);
    final what = await repository.whatOf(
      const TimesheetTemplate(
        id: 1,
        name: 'Template',
        taskId: 3,
        kost2Id: 7,
        description: 'D',
      ),
    );
    expect(what.task?.fullPath, 'ProjectForge › Dev');
    expect(what.kost2?.label, 'second');
    expect(what.description, 'D');
  });

  test('the running timer is kept in a file', () async {
    expect(await repository.loadTimer(), isNull);
    await repository.saveTimer(RunningTimer(DateTime(2031, 3, 10, 8, 3), null));
    expect((await repository.loadTimer())?.start, DateTime(2031, 3, 10, 8, 3));
    await repository.clearTimer();
    expect(await repository.loadTimer(), isNull);
  });
}
