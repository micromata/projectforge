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

import { useSyncExternalStore } from 'react';

/**
 * The sheets the tab bar moves up over the tabs: the notifications (the bell), the "more" menu and the sheets opened
 * from that menu (the settings, the server switch).
 */
export type TabSheetName = 'notifications' | 'more' | 'settings' | 'servers';

/** The button of the tab bar a sheet belongs to: the ones of the "more" menu to its button. */
export function buttonOf(sheet: TabSheetName | null): TabSheetName | null {
  return sheet === 'settings' || sheet === 'servers' ? 'more' : sheet;
}

/** Which sheet of the tab bar is shown over the tabs, at most one. */
class TabSheets {
  private current: TabSheetName | null = null;
  private listeners = new Set<() => void>();

  open(sheet: TabSheetName) {
    this.set(sheet);
  }

  /** Its button again: the sheet shown closes (also one opened from its menu), another one takes its place. */
  toggle(sheet: TabSheetName) {
    this.set(buttonOf(this.current) === sheet ? null : sheet);
  }

  close = () => this.set(null);

  get = () => this.current;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  private set(sheet: TabSheetName | null) {
    if (this.current === sheet) return;
    this.current = sheet;
    this.listeners.forEach((it) => it());
  }
}

export const tabSheets = new TabSheets();

export function useTabSheet(): TabSheetName | null {
  return useSyncExternalStore(tabSheets.subscribe, tabSheets.get);
}
