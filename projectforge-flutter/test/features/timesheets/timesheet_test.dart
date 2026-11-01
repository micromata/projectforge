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

import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/features/timesheets/time_utils.dart';
import 'package:projectforge_app/features/timesheets/timesheet.dart';

Map<String, dynamic> _sheet(
  int id,
  String start,
  String stop, {
  String task = 'Dev',
}) => {
  'id': id,
  'task': {'id': 3, 'title': task, 'path': 'ProjectForge -> Next'},
  'kost2': {
    'id': 7,
    'formattedNumber': '5.020.01.02',
    'description': 'Entwicklung',
  },
  'description': 'Tests',
  'startTime': DateTime(
    2031,
    3,
    10,
    int.parse(start.split(':')[0]),
    int.parse(start.split(':')[1]),
  ).toUtc().toIso8601String(),
  'stopTime': DateTime(
    2031,
    3,
    10,
    int.parse(stop.split(':')[0]),
    int.parse(stop.split(':')[1]),
  ).toUtc().toIso8601String(),
};

void main() {
  group('times', () {
    test('5-minute steps round down and up', () {
      expect(
        floor5(DateTime(2031, 3, 10, 8, 7, 30)),
        DateTime(2031, 3, 10, 8, 5),
      );
      expect(ceil5(DateTime(2031, 3, 10, 8, 7)), DateTime(2031, 3, 10, 8, 10));
      expect(ceil5(DateTime(2031, 3, 10, 8, 10)), DateTime(2031, 3, 10, 8, 10));
      expect(
        ceil5(DateTime(2031, 3, 10, 8, 10, 1)),
        DateTime(2031, 3, 10, 8, 15),
      );
      expect(ceil5(DateTime(2031, 3, 10, 8, 56)), DateTime(2031, 3, 10, 9));
    });

    test('the week starts on Monday, numbered as ISO 8601', () {
      expect(mondayOf(DateTime(2026, 11, 1)), DateTime(2026, 10, 26));
      expect(mondayOf(DateTime(2026, 10, 26)), DateTime(2026, 10, 26));
      expect(weekOfYear(DateTime(2026, 11, 1)), 44);
      expect(weekOfYear(DateTime(2026, 1, 1)), 1);
      expect(weekOfYear(DateTime(2027, 1, 1)), 53);
      expect(weekOfYear(DateTime(2024, 12, 30)), 1);
    });

    test('a duration is shown as h:mm', () {
      expect(formatDuration(const Duration(hours: 2, minutes: 5)), '2:05');
      expect(formatDuration(const Duration(hours: 36, minutes: 30)), '36:30');
    });
  });

  group('models', () {
    test(
      'a task of a list row keeps its path, a hit of the search splits it',
      () {
        final row = TaskRef.fromJson({
          'id': 3,
          'title': 'Dev',
          'path': 'ProjectForge -> Next',
        });
        expect(row.fullPath, 'ProjectForge › Next › Dev');
        expect(row.shortPath, 'Next › Dev');
        final hit = TaskRef.fromDisplayObject({
          'id': 3,
          'displayName': 'ProjectForge | Next | Dev',
        });
        expect(hit.title, 'Dev');
        expect(hit.parents, ['ProjectForge', 'Next']);
        expect(hit, row);
        final info = TaskInfo.fromJson({
          'id': 3,
          'title': 'Dev',
          'path': [
            {'id': 1, 'title': 'ProjectForge'},
          ],
          'kost2List': [
            {'id': 7, 'displayName': '5.020.01.02 - Entwicklung'},
          ],
        });
        expect(info.task.fullPath, 'ProjectForge › Dev');
        expect(info.kost2List.single.label, '5.020.01.02 - Entwicklung');
      },
    );

    test(
      'a time sheet keeps the json of the server and reads the local time',
      () {
        final json = _sheet(1, '8:00', '10:30')..['timeSavedByAI'] = 25;
        final sheet = Timesheet(json);
        expect(sheet.start, DateTime(2031, 3, 10, 8));
        expect(sheet.duration, const Duration(hours: 2, minutes: 30));
        expect(sheet.what.kost2?.label, '5.020.01.02 Entwicklung');
        expect(sheet.json['timeSavedByAI'], 25);
        expect(sheet.acceptancePending, isFalse);
      },
    );

    test('a week holds the bookings per day, their totals and the gaps', () {
      final monday = DateTime(2031, 3, 10);
      final week = TimesheetWeek.fromJson(monday, {
        'timesheets': [
          _sheet(1, '8:00', '10:30'),
          _sheet(2, '11:00', '12:00'),
          _sheet(3, '12:00', '13:00'),
        ],
        'tags': ['Remote'],
      });
      expect(week.bookingsOf(monday).map((it) => it.id), [1, 2, 3]);
      expect(week.bookingsOf(DateTime(2031, 3, 11)), isEmpty);
      expect(week.totalOf(monday), const Duration(hours: 4, minutes: 30));
      expect(week.tags, ['Remote']);
      // On another day than today only the gaps between the bookings.
      final gaps = week.gapsOf(monday, DateTime(2031, 3, 20, 9));
      expect(
        gaps.map(
          (it) =>
              (it.start.hour, it.start.minute, it.stop.hour, it.stop.minute),
        ),
        [(10, 30, 11, 0)],
      );
      // Today also from the last booking up to now.
      expect(
        week.gapsOf(monday, DateTime(2031, 3, 10, 15, 7)).last.stop,
        DateTime(2031, 3, 10, 15, 5),
      );
      // But not into the night.
      expect(
        week.gapsOf(monday, DateTime(2031, 3, 10, 23, 30)).last.stop,
        DateTime(2031, 3, 10, 20),
      );
      // A new booking continues after the last one, on an empty day at the start of the working day.
      expect(week.nextStartOf(monday), DateTime(2031, 3, 10, 13));
      expect(week.nextStartOf(DateTime(2031, 3, 11)), DateTime(2031, 3, 11, 8));
    });

    test('a recent entry of the server becomes what a booking fills in', () {
      final what = BookingWhat.fromTimesheetJson({
        'task': {'id': 3, 'title': 'Dev'},
        'tag': '',
        'location': 'Kassel',
      });
      expect(what.task?.id, 3);
      expect(what.tag, isNull);
      expect(what.location, 'Kassel');
      expect(BookingWhat.fromJson(what.toJson()).key, what.key);
    });

    test('the timer survives the file', () {
      final timer = RunningTimer(
        DateTime(2031, 3, 10, 8, 3),
        BookingWhat.fromTimesheetJson(_sheet(1, '8:00', '9:00')),
      );
      final back = RunningTimer.fromJson(timer.toJson());
      expect(back.start, timer.start);
      expect(back.what?.task?.id, 3);
    });
  });
}
