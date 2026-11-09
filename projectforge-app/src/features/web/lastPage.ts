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

import { JsonCache } from '../../core/cache/jsonCache';

const NAME = 'webLastPage';

/**
 * A page of next on the own server: not `//host` (another site) and not a path of an SSO proxy in front of it
 * (e.g. Authentik's `/outpost.goauthentik.io/start`, which would start the proxy's login again).
 */
const isServerPath = (path: unknown): path is string => typeof path === 'string' && /^\/next([/?#]|$)/.test(path);

/** The part of JsonCache used here. */
export interface PageCache {
  readSync<T>(name: string): T | null;
  write(name: string, json: unknown): void;
}

/**
 * The page shown last in the pages tab, its start page the next time. Data of the login, not a setting: kept in the
 * cache, which a logout and a switch to another server delete (a path of one user or server is not opened for another).
 */
export class LastPage {
  private last: string | null = null;

  constructor(private readonly cache: PageCache = JsonCache) {}

  /** [path]: of the own server, with its query and fragment. Written only if it changed. */
  remember = (path: string) => {
    if (path === this.last || !isServerPath(path)) return;
    this.last = path;
    this.cache.write(NAME, { path });
  };

  /** The page shown last, null if there is none (or it is broken). Read as the tab of a login starts. */
  restore(): string | null {
    let json: { path?: unknown } | null = null;
    try {
      json = this.cache.readSync<{ path?: unknown }>(NAME);
    } catch {
      // No cache to read: as if there were no page.
    }
    this.last = isServerPath(json?.path) ? json.path : null;
    return this.last;
  }
}

export const lastPage = new LastPage();
