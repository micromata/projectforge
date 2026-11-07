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

// Port of projectforge-flutter/test/core/compat/compatibility_test.dart.
import { compareVersions, compatibilityIssue, MIN_SERVER_VERSION, parseVersion } from '../compatibility';

test('versions are compared by their numeric segments', () => {
  expect(parseVersion('1.0.1+3')).toEqual([1, 0, 1]);
  expect(parseVersion('9.2.4-SNAPSHOT')).toEqual([9, 2, 4]);
  expect(parseVersion('<version>')).toBeNull();
  expect(parseVersion(null)).toBeNull();
  expect(compareVersions([1, 0], [1, 0, 0])).toBe(0);
  expect(compareVersions([1, 0, 9], [1, 0, 10])).toBeLessThan(0);
  expect(compareVersions([2], [1, 99])).toBeGreaterThan(0);
});

test('an app older than the server needs is too old', () => {
  expect(compatibilityIssue({ appVersion: '1.0.0', serverVersion: '99.0', minAppVersion: '1.0.1' })).toEqual({
    kind: 'appTooOld',
    minVersion: '1.0.1',
  });
});

test('a server older than the app needs is too old, a snapshot counts', () => {
  expect(compatibilityIssue({ appVersion: '1.1.0', serverVersion: '1.0', minAppVersion: '1.0.1' })?.kind).toBe(
    'serverTooOld',
  );
  expect(
    compatibilityIssue({ appVersion: '1.1.0', serverVersion: `${MIN_SERVER_VERSION}-SNAPSHOT`, minAppVersion: '1.0.1' }),
  ).toBeNull();
});

test('unknown versions never warn (an older server sends no minimum)', () => {
  expect(compatibilityIssue({ appVersion: '1.1.0', serverVersion: '<version>', minAppVersion: null })).toBeNull();
});
