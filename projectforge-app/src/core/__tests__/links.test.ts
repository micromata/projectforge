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

import { isChatPath, webPathOfLink } from '../web/links';

describe('webPathOfLink', () => {
  it('makes paths of the own server', () => {
    expect(webPathOfLink('/timesheet')).toBe('/next/timesheet');
    expect(webPathOfLink('timesheet/5')).toBe('/next/timesheet/5');
    expect(webPathOfLink('next/monthlyEmployeeReport')).toBe('/next/monthlyEmployeeReport');
    expect(webPathOfLink('react/calendar')).toBe('/react/calendar');
    expect(webPathOfLink('/chat/5?message=7')).toBe('/next/chat/5?message=7');
    expect(webPathOfLink(null)).toBeNull();
    expect(webPathOfLink(' ')).toBeNull();
  });

  it('never follows other hosts', () => {
    expect(webPathOfLink('https://evil.example.com/x')).toBeNull();
    expect(webPathOfLink('//evil.example.com/x')).toBeNull();
    expect(webPathOfLink('javascript:alert(1)')).toBeNull();
  });
});

describe('isChatPath', () => {
  it('tells the pages of the chat', () => {
    expect(isChatPath('/next/chat')).toBe(true);
    expect(isChatPath('/next/chat/5?message=7')).toBe(true);
    expect(isChatPath('/next/chatty')).toBe(false);
    expect(isChatPath('/next/timesheet')).toBe(false);
  });
});
