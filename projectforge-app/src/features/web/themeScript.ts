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

import type { ThemeMode } from '../../core/status/userStatus';

/** The key next-themes stores the theme of next's pages under (its default `storageKey`). */
const STORAGE_KEY = 'theme';

/**
 * Hands [theme] (chosen in the app, or changed on another device) to the page shown, a page of the own server: stored
 * where next-themes keeps it and announced by the `storage` event it listens to, so the page follows without a reload.
 * Nothing is done if the page has it already.
 */
export function themeScript(theme: ThemeMode): string {
  const key = JSON.stringify(STORAGE_KEY);
  const value = JSON.stringify(theme);
  return (
    `(function(){try{if(localStorage.getItem(${key})!==${value}){localStorage.setItem(${key},${value});` +
    `window.dispatchEvent(new StorageEvent('storage',{key:${key},newValue:${value}}));}}catch(e){}})();true;`
  );
}
