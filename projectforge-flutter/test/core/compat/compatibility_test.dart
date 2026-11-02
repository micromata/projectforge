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
import 'package:projectforge_app/core/compat/compatibility.dart';
import 'package:projectforge_app/core/status/user_status.dart';

void main() {
  test('versions are compared by their numeric segments', () {
    expect(parseVersion('1.0.1+3'), [1, 0, 1]);
    expect(parseVersion('9.2.4-SNAPSHOT'), [9, 2, 4]);
    expect(parseVersion('<version>'), isNull);
    expect(parseVersion(null), isNull);
    expect(compareVersions([1, 0], [1, 0, 0]), 0);
    expect(compareVersions([1, 0, 9], [1, 0, 10]), lessThan(0));
    expect(compareVersions([2], [1, 99]), greaterThan(0));
  });

  test('an app older than the server needs is too old', () {
    final issue = compatibilityIssue(
      appVersion: '1.0.0+7',
      serverVersion: '99.0',
      minAppVersion: '1.0.1',
    );
    expect(issue, isA<AppTooOld>());
    expect(issue!.minVersion, '1.0.1');
  });

  test('a server older than the app needs is too old, a snapshot counts', () {
    expect(
      compatibilityIssue(
        appVersion: '1.0.1+3',
        serverVersion: '1.0',
        minAppVersion: '1.0.1',
      ),
      isA<ServerTooOld>(),
    );
    expect(
      compatibilityIssue(
        appVersion: '1.0.1+3',
        serverVersion: '$minServerVersion-SNAPSHOT',
        minAppVersion: '1.0.1',
      ),
      isNull,
    );
  });

  test('unknown versions never warn (an older server sends no minimum)', () {
    expect(
      compatibilityIssue(
        appVersion: '1.0.1+3',
        serverVersion: '<version>',
        minAppVersion: null,
      ),
      isNull,
    );
  });

  test('the user status gives the versions of its systemData', () {
    final status = UserStatus({
      'systemData': {'version': '9.2.4', 'minAppVersion': '1.0.1'},
    });
    expect(status.serverVersion, '9.2.4');
    expect(status.minAppVersion, '1.0.1');
    expect(UserStatus({}).minAppVersion, isNull);
  });
}
