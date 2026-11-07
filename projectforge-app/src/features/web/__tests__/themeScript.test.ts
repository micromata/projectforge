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

import { themeScript } from '../themeScript';

/** Runs the script against a page whose next-themes stored [stored]; the events it announced. */
function run(theme: 'light' | 'dark' | 'system', stored: string | null) {
  const storage = new Map<string, string>(stored == null ? [] : [['theme', stored]]);
  const events: { key: string; newValue: string }[] = [];
  const page = {
    localStorage: {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (k: string, v: string) => storage.set(k, v),
    },
    window: { dispatchEvent: (event: { init: { key: string; newValue: string } }) => events.push(event.init) },
    StorageEvent: class {
      constructor(
        _type: string,
        public init: { key: string; newValue: string },
      ) {}
    },
  };
  new Function('localStorage', 'window', 'StorageEvent', themeScript(theme))(
    page.localStorage,
    page.window,
    page.StorageEvent,
  );
  return { stored: storage.get('theme'), events };
}

test('the page takes the theme and is told of it', () => {
  expect(run('dark', 'light')).toEqual({ stored: 'dark', events: [{ key: 'theme', newValue: 'dark' }] });
});

test('nothing happens if the page has it already', () => {
  expect(run('system', 'system').events).toEqual([]);
});
