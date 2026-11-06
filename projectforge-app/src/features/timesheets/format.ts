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

import { i18n } from '../../core/i18n/translations';

// Dates in the language of the app.

/** "Mo" */
export const weekdayShort = (d: Date) => d.toLocaleDateString(i18n.locale, { weekday: 'short' });

/** "Sonntag, 1. November" */
export const dayLong = (d: Date) => d.toLocaleDateString(i18n.locale, { weekday: 'long', day: 'numeric', month: 'long' });

/** "Mi., 4.11." */
export const dayShort = (d: Date) => `${weekdayShort(d)}, ${d.getDate()}.${d.getMonth() + 1}.`;

/** "November 2026" */
export const monthYear = (d: Date) => d.toLocaleDateString(i18n.locale, { month: 'long', year: 'numeric' });
