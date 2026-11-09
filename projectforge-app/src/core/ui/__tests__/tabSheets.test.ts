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

import { buttonOf, tabSheets } from '../tabSheets';

describe('tab sheets', () => {
  afterEach(() => tabSheets.close());

  it('shows one sheet at a time, its button again closes it', () => {
    const listener = jest.fn();
    const unsubscribe = tabSheets.subscribe(listener);
    tabSheets.toggle('notifications');
    expect(tabSheets.get()).toBe('notifications');
    tabSheets.toggle('more');
    expect(tabSheets.get()).toBe('more');
    tabSheets.toggle('more');
    expect(tabSheets.get()).toBeNull();
    expect(listener).toHaveBeenCalledTimes(3);
    unsubscribe();
  });

  it('closes a sheet of the "more" menu by its button, the bell replaces it', () => {
    tabSheets.open('settings');
    expect(buttonOf(tabSheets.get())).toBe('more');
    tabSheets.toggle('more');
    expect(tabSheets.get()).toBeNull();
    tabSheets.open('servers');
    tabSheets.toggle('notifications');
    expect(tabSheets.get()).toBe('notifications');
    expect(buttonOf(tabSheets.get())).toBe('notifications');
  });

  it('opens a shown sheet again without a change, closes any', () => {
    tabSheets.open('notifications');
    const listener = jest.fn();
    const unsubscribe = tabSheets.subscribe(listener);
    tabSheets.open('notifications');
    expect(listener).not.toHaveBeenCalled();
    tabSheets.close();
    expect(tabSheets.get()).toBeNull();
    expect(listener).toHaveBeenCalledTimes(1);
    unsubscribe();
  });
});
