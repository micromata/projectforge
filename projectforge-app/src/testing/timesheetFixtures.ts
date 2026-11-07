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

import type { TimesheetData } from '@projectforge/shared/rs/timesheet';

/** A booking on 10 March 2031 (a Monday), local time. */
export function sheet(id: number, start: string, stop: string): TimesheetData & { timeSavedByAI?: number } {
  const at = (time: string) => {
    const [h, m] = time.split(':').map(Number);
    return new Date(2031, 2, 10, h, m).toISOString();
  };
  return {
    id,
    task: { id: 3, title: 'Dev', path: 'ProjectForge -> Next' },
    kost2: { id: 7, formattedNumber: '5.020.01.02', description: 'Entwicklung' },
    description: 'Tests',
    startTime: at(start),
    stopTime: at(stop),
  };
}
