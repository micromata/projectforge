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

/**
 * The page the system browser shows for `{"pf":"openInBrowser","path":…}` of a page: the page [path] of next at
 * [server] (below its context path) if the page named one (e.g. `/next/2FASetup`, where passkeys can be registered, which
 * the WebView can't), else the page [shown] now. Anything but a path of next is ignored.
 */
export function browserTarget(server: URL, shown: string | null, path: unknown): string | null {
  if (typeof path === 'string' && /^\/next\/[\w\-./?=&%#]*$/.test(path) && !path.includes('..')) {
    return `${server.toString().replace(/\/+$/, '')}${path}`;
  }
  return shown;
}
