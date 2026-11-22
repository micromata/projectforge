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

import { i18n } from './translations';

/**
 * A day as the server sends it (`yyyy-MM-dd`, e.g. a birthday) in the format of the language; anything else as is.
 * Without the year (day and month only) for `yearUnknown` (the year is then only a placeholder) and for `--MM-dd`.
 */
export function formatIsoDay(iso: string, yearUnknown = false): string {
  const partial = /^--(\d{2})-(\d{2})$/.exec(iso);
  if (partial || (yearUnknown && /^\d{4}-\d{2}-\d{2}$/.test(iso))) {
    const [month, day] = partial ? [partial[1], partial[2]] : [iso.slice(5, 7), iso.slice(8, 10)];
    // 2000 is a leap year, so 29 February stays valid.
    return new Date(2000, Number(month) - 1, Number(day)).toLocaleDateString(i18n.locale, {
      day: 'numeric',
      month: 'long',
    });
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
  if (!match) return iso;
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  return date.toLocaleDateString(i18n.locale, { dateStyle: 'medium' });
}
