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
 * The page of the app a link of a notification or a push opens, as a path of the server: the links are paths of the
 * next app (`/timesheet`) or of the legacy app (`react/...`); other servers (absolute urls) aren't followed.
 */
export function webPathOfLink(link?: string | null): string | null {
  const value = link?.trim();
  if (!value) return null;
  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(value) || value.startsWith('//')) return null;
  const relative = value.startsWith('/') ? value.substring(1) : value;
  if (relative.startsWith('next/') || relative.startsWith('react/') || relative === 'next') return `/${relative}`;
  return `/next/${relative}`;
}

/** True if [path] (see [webPathOfLink]) is a page of the chat, which has a tab of its own. */
export function isChatPath(path: string): boolean {
  return path === '/next/chat' || path.startsWith('/next/chat/') || path.startsWith('/next/chat?');
}
