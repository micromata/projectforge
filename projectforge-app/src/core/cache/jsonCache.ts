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

import { Directory, File, Paths } from 'expo-file-system';

import { appLog } from '../log/appLog';

/**
 * Json documents the app shows when the server isn't reachable (e.g. the business cards at a fair without network,
 * the weeks of time sheets last seen). Kept in the app's own documents (no other app reads them), deleted on logout and
 * on a switch to another server (the cache isn't kept per server). Port of the Flutter app's JsonCache.
 */
export const JsonCache = {
  async read<T = Record<string, unknown>>(name: string): Promise<T | null> {
    const file = new File(cacheDir(), `${name}.json`);
    if (!file.exists) return null;
    try {
      return JSON.parse(await file.text()) as T;
    } catch {
      return null; // Broken or of an older version: as if there were none.
    }
  },

  write(name: string, json: unknown): void {
    const dir = cacheDir();
    dir.create({ intermediates: true, idempotent: true });
    const file = new File(dir, `${name}.json`);
    file.create({ overwrite: true });
    file.write(JSON.stringify(json));
  },

  delete(name: string): void {
    const file = new File(cacheDir(), `${name}.json`);
    if (file.exists) file.delete();
  },

  /** A directory of the cache for files of its own (images, shared files), deleted with it. */
  directory(name: string): Directory {
    const dir = new Directory(cacheDir(), name);
    dir.create({ intermediates: true, idempotent: true });
    return dir;
  },

  clearAll(): void {
    try {
      const dir = cacheDir();
      if (dir.exists) dir.delete();
    } catch (e) {
      appLog.warn('cache', `Cache not deleted: ${String(e)}`);
    }
  },
};

const cacheDir = () => new Directory(Paths.document, 'pf-cache');
