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

import type { Menus } from '@projectforge/shared/rs/menu';

import { flattenMenuEntries, menuPath, MenuRow, menuRows, searchMenuEntries } from '../menuModel';
import { webTabs } from '../webTabs';

const labels = { mainMenu: 'Main menu', favorites: 'Favorites', myAccount: 'My account', recent: 'Recently used' };

const menus: Menus = {
  mainMenu: {
    menuItems: [
      {
        key: 'COMMON',
        title: 'Common',
        subMenu: [
          { key: 'COMMON.ADDRESS_LIST', title: 'Addresses', url: 'next/address' },
          { key: 'COMMON.CALENDAR', title: 'Calendar', url: 'next/calendar' },
        ],
      },
      {
        key: 'FIBU',
        title: 'Fibu',
        badge: { counter: 3 },
        subMenu: [
          { key: 'FIBU.KREDITOR', title: 'Kreditoren-Adressbuch', url: 'next/creditor' },
          { key: 'FIBU.BUCHUNG', title: 'Buchungskonten', url: 'next/account', badge: { counter: 3 } },
        ],
      },
    ],
  },
  favoritesMenu: {
    menuItems: [
      { key: 'ADDRESS_LIST', title: 'Addresses', url: 'next/address' },
      { key: 'folder', title: 'Reports', subMenu: [{ key: 'REPORT', title: 'Report', url: 'next/report' }] },
    ],
  },
  myAccountMenu: {
    menuItems: [
      {
        key: 'MY_MENU',
        title: 'Kai Example',
        subMenu: [
          { key: 'MY_ACCOUNT', title: 'My account', url: 'next/myAccount' },
          { key: 'LOGOUT', title: 'Logout', url: 'rs/logout', type: 'RESTCALL' },
        ],
      },
    ],
  },
  recentMenu: { menuItems: [{ key: 'CALENDAR', title: 'Calendar', url: 'next/calendar' }] },
};

const titles = (rows: MenuRow[]) =>
  rows.map((it) =>
    it.kind === 'entry' ? it.entry.title : it.kind === 'heading' || it.kind === 'group' ? `[${it.title}]` : it.kind,
  );

describe('flattenMenuEntries', () => {
  test('every page once, the main menu first, without the logout', () => {
    const entries = flattenMenuEntries(menus, labels);
    expect(entries.map((it) => `${it.category}/${it.title}`)).toEqual([
      'Common/Addresses',
      'Common/Calendar',
      'Fibu/Kreditoren-Adressbuch',
      'Fibu/Buchungskonten',
      'Reports/Report',
      'My account/My account',
    ]);
    expect(entries[0].menuKey).toBe('COMMON.ADDRESS_LIST');
  });
});

describe('searchMenuEntries', () => {
  test('the start of the title before a word start before anywhere', () => {
    const entries = flattenMenuEntries(menus, labels);
    expect(searchMenuEntries(entries, 'buch').map((it) => it.title)).toEqual([
      'Buchungskonten',
      'Kreditoren-Adressbuch',
    ]);
    expect(searchMenuEntries(entries, 'adress').map((it) => it.title)).toEqual(['Kreditoren-Adressbuch']);
  });

  test('the category finds its entries', () => {
    expect(searchMenuEntries(flattenMenuEntries(menus, labels), 'FIBU')).toHaveLength(2);
  });
});

describe('menuRows', () => {
  const closed = { term: '', openGroups: new Set<string>(), accountOpen: false };

  test('the entries opened last, the favorites with their folders and the closed categories', () => {
    expect(titles(menuRows(menus, labels, closed))).toEqual([
      '[Recently used]',
      'Calendar',
      '[Favorites]',
      'Addresses',
      '[Reports]',
      'Report',
      '[Main menu]',
      '[Common]',
      '[Fibu]',
    ]);
  });

  test('an open category shows its entries, the open account menu comes first', () => {
    const rows = menuRows(menus, labels, { term: '', openGroups: new Set(['FIBU']), accountOpen: true });
    expect(titles(rows).slice(0, 2)).toEqual(['[My account]', 'My account']);
    expect(titles(rows).slice(-3)).toEqual(['[Fibu]', 'Kreditoren-Adressbuch', 'Buchungskonten']);
    expect(rows.find((it) => it.kind === 'group' && it.key === 'FIBU')).toMatchObject({ open: true, badgeCounter: 3 });
  });

  test('without recent entries (an older server) the section is left out', () => {
    const { recentMenu: _, ...older } = menus;
    expect(titles(menuRows(older, labels, closed))[0]).toBe('[Favorites]');
  });

  test('searching shows the hits by category and the search in all data', () => {
    expect(titles(menuRows(menus, labels, { ...closed, term: ' cal ' }))).toEqual([
      '[Common]',
      'Calendar',
      'searchData',
    ]);
    const none = menuRows(menus, labels, { ...closed, term: 'xyz' });
    expect(titles(none)).toEqual(['noMatch', 'searchData']);
    expect(none[1]).toMatchObject({ term: 'xyz' });
  });
});

describe('menuPath', () => {
  test('a path of the server gets its slash, another site stays', () => {
    expect(menuPath('next/address')).toBe('/next/address');
    expect(menuPath('/next/address')).toBe('/next/address');
    expect(menuPath('https://example.org/x')).toBe('https://example.org/x');
  });
});

describe('the menu of the pages tab', () => {
  beforeEach(() => webTabs.reset());

  test('shown at the start, toggled by the tab', () => {
    expect(webTabs.getMenu()).toBe(true);
    webTabs.toggleMenu();
    expect(webTabs.getMenu()).toBe(false);
    webTabs.toggleMenu();
    expect(webTabs.getMenu()).toBe(true);
  });

  test('a page asked for closes it, also before the tab exists', () => {
    webTabs.open('pages', '/next/address/edit/1');
    expect(webTabs.getMenu()).toBe(false);
    expect(webTabs.takePending('pages')).toBe('/next/address/edit/1');
  });

  test('the chat leaves it alone', () => {
    webTabs.open('chat', '/next/chat/1');
    expect(webTabs.getMenu()).toBe(true);
  });
});
