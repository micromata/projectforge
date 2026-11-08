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


// Port of the earlier Flutter app's app_settings_test (the parsing; the file is expo-file-system's).
import { NATIVE_PAGES } from '../../web/nativePage';
import { opensInApp, settingsFromJson } from '../appSettings';

test('everything opens in the app by default', () => {
  const settings = settingsFromJson(null);
  for (const page of NATIVE_PAGES) expect(opensInApp(settings, page)).toBe(true);
});

test('a page kept in the web version', () => {
  const settings = settingsFromJson({ webVersion: ['businessCards'] });
  expect(opensInApp(settings, 'businessCards')).toBe(false);
  expect(opensInApp(settings, 'chat')).toBe(true);
});

test('unknown or broken settings give the defaults', () => {
  expect(settingsFromJson({ webVersion: ['chat', 'gone'] }).webVersion).toEqual(['chat']);
  expect(settingsFromJson({ webVersion: 'chat' }).webVersion).toEqual([]);
  expect(settingsFromJson('{').webVersion).toEqual([]);
});

test('the navigation in landscape at the left unless chosen right', () => {
  expect(settingsFromJson(null).railSide).toBe('left');
  expect(settingsFromJson({ webVersion: [] }).railSide).toBe('left');
  expect(settingsFromJson({ railSide: 'right' }).railSide).toBe('right');
  expect(settingsFromJson({ railSide: 'top' }).railSide).toBe('left');
});
