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

import { ceil5, floor5, formatDuration, mondayOf, parseTimeGrid, weekOfYear, weekStartOf } from '@projectforge/shared/booking-time';
import {
  acceptancePending,
  bookingsOf,
  durationOf,
  fullPath,
  gapsOf,
  kost2Number,
  kost2OfTaskInfo,
  kost2OfTimesheet,
  kost2Text,
  nextStartOf,
  shortPath,
  storedTimer,
  targetOf,
  taskInfoOf,
  taskOfDisplayObject,
  taskOfTimesheet,
  timerOf,
  timesheetOf,
  totalOf,
  weekOf,
  whatKey,
  whatOfTimesheet,
} from '../timesheet';
import { sheet } from '../../../testing/timesheetFixtures';

describe('booking times', () => {
  it('rounds 5-minute steps down and up', () => {
    expect(floor5(new Date(2031, 2, 10, 8, 7, 30))).toEqual(new Date(2031, 2, 10, 8, 5));
    expect(ceil5(new Date(2031, 2, 10, 8, 7))).toEqual(new Date(2031, 2, 10, 8, 10));
    expect(ceil5(new Date(2031, 2, 10, 8, 10))).toEqual(new Date(2031, 2, 10, 8, 10));
    expect(ceil5(new Date(2031, 2, 10, 8, 10, 1))).toEqual(new Date(2031, 2, 10, 8, 15));
    expect(ceil5(new Date(2031, 2, 10, 8, 56))).toEqual(new Date(2031, 2, 10, 9));
  });

  it('rounds to the grid of the installation', () => {
    expect(floor5(new Date(2031, 2, 10, 8, 29, 59), 15)).toEqual(new Date(2031, 2, 10, 8, 15));
    expect(ceil5(new Date(2031, 2, 10, 8, 31), 15)).toEqual(new Date(2031, 2, 10, 8, 45));
    expect(ceil5(new Date(2031, 2, 10, 8, 30), 15)).toEqual(new Date(2031, 2, 10, 8, 30));
    expect(ceil5(new Date(2031, 2, 10, 8, 46), 15)).toEqual(new Date(2031, 2, 10, 9));
    expect(ceil5(new Date(2031, 2, 10, 8, 10), 30)).toEqual(new Date(2031, 2, 10, 8, 30));
  });

  it('takes a valid grid, else 5 minutes', () => {
    expect([15, 30, 10, 5].map(parseTimeGrid)).toEqual([15, 30, 10, 5]);
    expect([undefined, null, 0, 7, 45, 12.5, 60].map(parseTimeGrid)).toEqual([5, 5, 5, 5, 5, 5, 5]);
  });

  it('starts the week on Monday, numbered as ISO 8601', () => {
    expect(mondayOf(new Date(2026, 10, 1))).toEqual(new Date(2026, 9, 26));
    expect(mondayOf(new Date(2026, 9, 26))).toEqual(new Date(2026, 9, 26));
    expect(weekOfYear(new Date(2026, 10, 1))).toBe(44);
    expect(weekOfYear(new Date(2026, 0, 1))).toBe(1);
    expect(weekOfYear(new Date(2027, 0, 1))).toBe(53);
    expect(weekOfYear(new Date(2024, 11, 30))).toBe(1);
  });

  it('starts the week on the user\'s first day of the week', () => {
    expect(weekStartOf(new Date(2026, 10, 1, 14, 30), 1)).toEqual(new Date(2026, 9, 26));
    expect(weekStartOf(new Date(2026, 10, 1, 14, 30), 7)).toEqual(new Date(2026, 10, 1));
    expect(weekStartOf(new Date(2026, 9, 31), 7)).toEqual(new Date(2026, 9, 25));
    expect(weekStartOf(new Date(2026, 10, 1), 6)).toEqual(new Date(2026, 9, 31));
    expect(weekStartOf(new Date(2026, 9, 30), 6)).toEqual(new Date(2026, 9, 24));
  });

  it('shows a duration as h:mm', () => {
    expect(formatDuration(125)).toBe('2:05');
    expect(formatDuration(36 * 60 + 30)).toBe('36:30');
  });
});

describe('time sheet models', () => {
  it('keeps the path of the task of a list row, splits the one of a hit of the search', () => {
    const row = taskOfTimesheet({ id: 3, title: 'Dev', path: 'ProjectForge -> Next' });
    expect(fullPath(row)).toBe('ProjectForge › Next › Dev');
    expect(shortPath(row)).toBe('Next › Dev');
    const hit = taskOfDisplayObject({ id: 3, displayName: 'ProjectForge | Next | Dev' });
    expect(hit).toEqual(row);
    const info = taskInfoOf({
      id: 3,
      title: 'Dev',
      path: [{ id: 1, title: 'ProjectForge' }],
      kost2List: [{ id: 7, title: '5.020.01.02 - Entwicklung' }],
    });
    expect(fullPath(info.task)).toBe('ProjectForge › Dev');
    expect(info.pathIds).toEqual([1]);
    expect(info.kost2List[0].label).toBe('5.020.01.02 - Entwicklung');
  });

  it('labels a cost unit by its number, else by its display name, never by its id', () => {
    expect(kost2OfTimesheet({ id: 7, formattedNumber: '5.020.01.02', description: 'Dev' }).label).toBe('5.020.01.02 Dev');
    // The rows of appList: only the display name.
    expect(kost2OfTimesheet({ id: 7, displayName: '5.020.01.02: Entwicklung - PF' }).label).toBe(
      '5.020.01.02: Entwicklung - PF',
    );
    expect(kost2OfTimesheet({ id: 7 }).label).toBe('');
  });

  it('keeps the json of the server and reads the local time', () => {
    const json = { ...sheet(1, '8:00', '10:30'), timeSavedByAI: 25 };
    const it = timesheetOf(json);
    expect(it.start).toEqual(new Date(2031, 2, 10, 8));
    expect(durationOf(it)).toBe(150);
    expect(it.what.kost2?.label).toBe('5.020.01.02 Entwicklung');
    expect((it.json as { timeSavedByAI?: number }).timeSavedByAI).toBe(25);
    expect(acceptancePending(it)).toBe(false);
  });

  it('reads a cost unit of task/info from its title', () => {
    const kost2 = kost2OfTaskInfo({ id: 7, title: '5.020.01.02: Entwicklung - Projekt' });
    expect(kost2Number(kost2)).toBe('5.020.01.02');
    expect(kost2Text(kost2)).toBe('Entwicklung - Projekt');
    expect(kost2Text(kost2OfTaskInfo({ id: 8, title: '5.020.01.03' }))).toBe('');
  });

  it('holds the bookings of a week per day, their totals and the gaps', () => {
    const monday = new Date(2031, 2, 10);
    const week = weekOf(monday, {
      timesheets: [sheet(1, '8:00', '10:30'), sheet(2, '11:00', '12:00'), sheet(3, '12:00', '13:00')],
      tags: ['Remote'],
    });
    expect(bookingsOf(week, monday).map((it) => it.id)).toEqual([1, 2, 3]);
    expect(bookingsOf(week, new Date(2031, 2, 11))).toEqual([]);
    expect(totalOf(bookingsOf(week, monday))).toBe(270);
    expect(week.tags).toEqual(['Remote']);
    // On another day than today only the gaps between the bookings.
    const gaps = gapsOf(week, monday, new Date(2031, 2, 20, 9));
    expect(gaps.map((it) => [it.start.getHours(), it.start.getMinutes(), it.stop.getHours(), it.stop.getMinutes()])).toEqual([
      [10, 30, 11, 0],
    ]);
    // Today also from the last booking up to now, but not into the night.
    expect(gapsOf(week, monday, new Date(2031, 2, 10, 15, 7)).at(-1)?.stop).toEqual(new Date(2031, 2, 10, 15, 5));
    expect(gapsOf(week, monday, new Date(2031, 2, 10, 23, 30)).at(-1)?.stop).toEqual(new Date(2031, 2, 10, 20));
    // A new booking continues after the last one, on an empty day at the start of the working day.
    expect(nextStartOf(week, monday)).toEqual(new Date(2031, 2, 10, 13));
    expect(nextStartOf(week, new Date(2031, 2, 11))).toEqual(new Date(2031, 2, 11, 8));
  });

  it('measures each day against the target the server sent for it, 8 hours without', () => {
    const monday = new Date(2031, 2, 10);
    // The weekly hours change from 40 to 30 on Wednesday.
    const week = weekOf(monday, {
      timesheets: [],
      targetMinutes: { '2031-03-10': 480, '2031-03-11': 480, '2031-03-12': 360 },
    });
    expect(targetOf(week, new Date(2031, 2, 11, 14))).toBe(480);
    expect(targetOf(week, new Date(2031, 2, 12))).toBe(360);
    expect(targetOf(week, new Date(2031, 2, 16))).toBe(480);
    // An older server, or a week cached before.
    expect(targetOf(weekOf(monday, { timesheets: [] }), new Date(2031, 2, 12))).toBe(480);
    expect(targetOf(null, monday)).toBe(480);
  });

  it('turns a recent entry of the server into what a booking fills in', () => {
    const what = whatOfTimesheet({ task: { id: 3, title: 'Dev' }, tag: '', location: 'Kassel' });
    expect(what.task?.id).toBe(3);
    expect(what.tag).toBeNull();
    expect(what.location).toBe('Kassel');
    expect(whatKey(JSON.parse(JSON.stringify(what)))).toBe(whatKey(what));
  });

  it('keeps the timer in its file', () => {
    const timer = { start: new Date(2031, 2, 10, 8, 3), what: whatOfTimesheet(sheet(1, '8:00', '9:00')) };
    const back = timerOf(JSON.parse(JSON.stringify(storedTimer(timer))));
    expect(back.start).toEqual(timer.start);
    expect(back.what?.task?.id).toBe(3);
  });
});
