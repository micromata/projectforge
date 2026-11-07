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
 * A page of the server the app has a tab of its own for, opened there instead (unless switched off in the settings).
 * Not the time sheets: the booking of the app's tab is no counterpart of the web list, which stays a web page.
 */
export type NativePage = 'chat' | 'businessCards' | 'notifications' | 'addresses';

export const NATIVE_PAGES: readonly NativePage[] = ['chat', 'businessCards', 'notifications', 'addresses'];

/** Whether the time sheet tab is offered; `false` hides it. */
export const TIMESHEETS_TAB_ENABLED = true;
