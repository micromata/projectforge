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

import 'package:intl/intl.dart';

/// Dates and times of the time sheets on the device's clock. The server books 5-minute steps without seconds.
const bookingStepMinutes = 5;

/// The working hours of a day the week strip measures against.
const targetPerDay = Duration(hours: 8);

/// The start of a new booking on an empty day.
const defaultFirstHour = 8;

DateTime dayOf(DateTime t) => DateTime(t.year, t.month, t.day);

/// [days] later at the same time of the day (not 24 hours: a change of daylight saving time lies between).
DateTime addDays(DateTime t, int days) =>
    DateTime(t.year, t.month, t.day + days, t.hour, t.minute);

DateTime mondayOf(DateTime t) =>
    DateTime(t.year, t.month, t.day - (t.weekday - DateTime.monday));

DateTime floor5(DateTime t) => DateTime(
  t.year,
  t.month,
  t.day,
  t.hour,
  t.minute - t.minute % bookingStepMinutes,
);

DateTime ceil5(DateTime t) {
  final floor = floor5(t);
  final exact =
      floor.minute == t.minute &&
      t.second == 0 &&
      t.millisecond == 0 &&
      t.microsecond == 0;
  return exact ? floor : floor.add(const Duration(minutes: bookingStepMinutes));
}

/// ISO 8601 week of the year.
int weekOfYear(DateTime date) {
  final thursday = DateTime(
    date.year,
    date.month,
    date.day + DateTime.thursday - date.weekday,
  );
  // Rounded: a change of daylight saving time makes a day 23 or 25 hours long.
  final dayOfYear =
      (thursday.difference(DateTime(thursday.year, 1, 1)).inHours / 24).round();
  return 1 + dayOfYear ~/ 7;
}

String two(int n) => n.toString().padLeft(2, '0');

String formatTime(DateTime t) => '${two(t.hour)}:${two(t.minute)}';

/// `h:mm`
String formatDuration(Duration d) => '${d.inHours}:${two(d.inMinutes % 60)}';

/// Short weekday ("Mo") in [locale] (`en` or `de`).
String weekdayShort(String locale, DateTime d) =>
    DateFormat.E(locale).format(d);

/// "Sonntag, 1. November"
String dayLong(String locale, DateTime d) =>
    DateFormat.MMMMEEEEd(locale).format(d);

/// "Mi, 4.11."
String dayShort(String locale, DateTime d) =>
    '${DateFormat.E(locale).format(d)}, ${d.day}.${d.month}.';

/// "November 2026"
String monthYear(String locale, DateTime d) =>
    DateFormat.yMMMM(locale).format(d);
