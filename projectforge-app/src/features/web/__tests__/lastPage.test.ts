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

import { LastPage, PageCache } from '../lastPage';

function memoryCache(): PageCache & { writes: number; files: Map<string, unknown> } {
  const files = new Map<string, unknown>();
  return {
    files,
    writes: 0,
    readSync<T>(name: string) {
      return (files.get(name) as T) ?? null;
    },
    write(name, json) {
      this.writes++;
      files.set(name, json);
    },
  };
}

describe('the page shown last in the pages tab', () => {
  test('comes back as the start page, written only when it changes', () => {
    const cache = memoryCache();
    const pages = new LastPage(cache);
    expect(pages.restore()).toBeNull();
    pages.remember('/next/address/edit/1?tab=2#x');
    pages.remember('/next/address/edit/1?tab=2#x');
    expect(cache.writes).toBe(1);
    expect(new LastPage(cache).restore()).toBe('/next/address/edit/1?tab=2#x');
  });

  test('only a path of the own server', () => {
    const cache = memoryCache();
    const pages = new LastPage(cache);
    pages.remember('//other.example.org/');
    pages.remember('https://other.example.org/');
    expect(cache.writes).toBe(0);
    cache.files.set('webLastPage', { path: '//other.example.org/' });
    expect(pages.restore()).toBeNull();
    cache.files.set('webLastPage', 'broken');
    expect(pages.restore()).toBeNull();
  });

  test('only a page of next, not a path of an SSO proxy', () => {
    const cache = memoryCache();
    const pages = new LastPage(cache);
    pages.remember('/outpost.goauthentik.io/start?rd=%2Fnext%2Ftask');
    pages.remember('/nextcloud/');
    expect(cache.writes).toBe(0);
    cache.files.set('webLastPage', { path: '/outpost.goauthentik.io/start?rd=%2Fnext%2Ftask' });
    expect(pages.restore()).toBeNull();
    pages.remember('/next');
    pages.remember('/next?x=1');
    expect(cache.writes).toBe(2);
  });

  test('written again after the cache was deleted (a new login)', () => {
    const cache = memoryCache();
    const pages = new LastPage(cache);
    pages.remember('/next/task');
    cache.files.clear();
    expect(pages.restore()).toBeNull();
    pages.remember('/next/task');
    expect(cache.writes).toBe(2);
  });
});
