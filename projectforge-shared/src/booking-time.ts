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

// Dates and times of time sheets on the device's clock (local time). The server books 5-minute steps without
// seconds. Durations are minutes.

export const BOOKING_STEP_MINUTES = 5;

/** The working minutes of a day the week strip measures against. */
export const TARGET_MINUTES_PER_DAY = 8 * 60;

/** The start of a new booking on an empty day. */
export const DEFAULT_FIRST_HOUR = 8;

/** The day of [t] at midnight. */
export function dayOf(t: Date): Date {
  return new Date(t.getFullYear(), t.getMonth(), t.getDate());
}

export function sameDay(a: Date, b: Date): boolean {
  return dayOf(a).getTime() === dayOf(b).getTime();
}

/** [days] later at the same time of the day (not 24 hours: a change of daylight saving time may lie between). */
export function addDays(t: Date, days: number): Date {
  return new Date(t.getFullYear(), t.getMonth(), t.getDate() + days, t.getHours(), t.getMinutes());
}

export function addMinutes(t: Date, minutes: number): Date {
  return new Date(t.getTime() + minutes * 60_000);
}

/** The whole days from [a] to [b] (midnights), rounded: a day of a change of daylight saving time has 23 or 25 hours. */
export function daysBetween(a: Date, b: Date): number {
  return Math.round((dayOf(b).getTime() - dayOf(a).getTime()) / 86_400_000);
}

/** The Monday of the week of [t]. */
export function mondayOf(t: Date): Date {
  return weekStartOf(t, 1);
}

/** The first day of the week of [t] at midnight, for weeks starting on [isoFirstDay] (Monday 1 … Sunday 7). */
export function weekStartOf(t: Date, isoFirstDay = 1): Date {
  const offset = (isoWeekday(t) - isoFirstDay + 7) % 7;
  return new Date(t.getFullYear(), t.getMonth(), t.getDate() - offset);
}

/** Monday 1 … Sunday 7, as ISO 8601. */
export function isoWeekday(t: Date): number {
  return ((t.getDay() + 6) % 7) + 1;
}

/**
 * The grid of the times in minutes the installation configured (a divisor of 60 that is a multiple of 5), the
 * default [BOOKING_STEP_MINUTES] for a missing or invalid value, e.g. from an older server.
 */
export function parseTimeGrid(value?: number | null): number {
  return value != null && Number.isInteger(value) && value >= BOOKING_STEP_MINUTES && value <= 30 && 60 % value === 0
    ? value
    : BOOKING_STEP_MINUTES;
}

/** [t] rounded down to the grid [step] (minutes), seconds dropped. */
export function floor5(t: Date, step = BOOKING_STEP_MINUTES): Date {
  return new Date(
    t.getFullYear(),
    t.getMonth(),
    t.getDate(),
    t.getHours(),
    t.getMinutes() - (t.getMinutes() % step),
  );
}

/** [t] rounded up to the grid [step] (minutes). */
export function ceil5(t: Date, step = BOOKING_STEP_MINUTES): Date {
  const floor = floor5(t, step);
  return floor.getTime() === t.getTime() ? floor : addMinutes(floor, step);
}

/** The minutes from [start] to [stop]. */
export function minutesBetween(start: Date, stop: Date): number {
  return Math.round((stop.getTime() - start.getTime()) / 60_000);
}

/** ISO 8601 week of the year. */
export function weekOfYear(date: Date): number {
  const thursday = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 4 - isoWeekday(date));
  return 1 + Math.floor(daysBetween(new Date(thursday.getFullYear(), 0, 1), thursday) / 7);
}

export function two(n: number): string {
  return String(n).padStart(2, '0');
}

/** `08:05` */
export function formatTime(t: Date): string {
  return `${two(t.getHours())}:${two(t.getMinutes())}`;
}

/** `h:mm` of [minutes]. */
export function formatDuration(minutes: number): string {
  const sign = minutes < 0 ? '-' : '';
  const abs = Math.abs(minutes);
  return `${sign}${Math.floor(abs / 60)}:${two(abs % 60)}`;
}
