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

import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/log/app_log.dart';

void main() {
  test('entries older than a day are dropped', () {
    var now = DateTime(2026, 11, 2, 10);
    final log = AppLog(clock: () => now)..info('app', 'old');
    now = now.add(const Duration(hours: 23));
    log.info('app', 'recent');
    expect(log.entries.map((it) => it.message), ['old', 'recent']);
    now = now.add(const Duration(hours: 2));
    expect(log.entries.map((it) => it.message), ['recent']);
  });

  test('at most maxEntries, the oldest go first', () {
    final log = AppLog(maxEntries: 3);
    for (var i = 0; i < 5; i++) {
      log.debug('app', 'entry $i');
    }
    expect(log.entries.map((it) => it.message), [
      'entry 2',
      'entry 3',
      'entry 4',
    ]);
  });

  test('messages are sanitized, text renders all entries', () {
    final log = AppLog(clock: () => DateTime(2026, 11, 2, 10, 5))
      ..warn('api', 'Authorization: PFApp secret')
      ..error('auth', 'Failed', StateError('token=abc'));
    final text = log.text();
    expect(text, isNot(contains('secret')));
    expect(text, isNot(contains('abc')));
    expect(text, contains('WARN  [api] Authorization: PFApp ***'));
    expect(text, contains('ERROR [auth] Failed: StateError'));
  });

  test(
    'listeners are notified once for several entries, after the current task',
    () async {
      final log = AppLog();
      var notified = 0;
      log.addListener(() => notified++);
      log
        ..info('app', 'a')
        ..info('app', 'b');
      expect(notified, 0);
      await Future<void>.delayed(Duration.zero);
      expect(notified, 1);
      log.clear();
      await Future<void>.delayed(Duration.zero);
      expect(notified, 2);
      expect(log.entries, isEmpty);
    },
  );
}
