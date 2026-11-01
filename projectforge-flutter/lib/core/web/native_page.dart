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
/// A page of the server the app has a tab of its own for.
enum NativePage { chat, businessCards, notifications, addresses, timesheets }

/// The time sheet tab is unfinished: hidden (tab, link of the web pages, setting) until it is tried against a real
/// server.
const timesheetsTabEnabled = false;

/// The pages with a native tab the settings can switch.
Iterable<NativePage> get switchableNativePages => NativePage.values.where(
  (it) => it != NativePage.timesheets || timesheetsTabEnabled,
);
