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

import {
  addMinutes,
  BOOKING_STEP_MINUTES,
  dayOf,
  DEFAULT_FIRST_HOUR,
  floor5,
  minutesBetween,
  sameDay,
  TARGET_MINUTES_PER_DAY,
  two,
} from '@projectforge/shared/booking-time';
import type {
  AppTimesheetList,
  DisplayObject,
  TaskInfo as TaskInfoData,
  TimesheetData,
  TimesheetFavorite,
  TimesheetKost2,
  TimesheetTask,
  TaskKost2,
} from '@projectforge/shared/rs/timesheet';

const SEPARATOR = /\s*(?:\||->)\s*/;

/** A task (structure element) as the app shows it: its title and the titles of the tasks above it. */
export interface TaskRef {
  id: number;
  title: string;
  parents: string[];
}

/** The task of a booking: its ancestors as one text (`A -> B`). */
export function taskOfTimesheet(task: TimesheetTask): TaskRef {
  return {
    id: task.id,
    title: task.title ?? `#${task.id}`,
    parents: task.path ? task.path.split(SEPARATOR) : [],
  };
}

/** A hit of the task search, the whole path as `A | B | C`. */
export function taskOfDisplayObject(json: DisplayObject): TaskRef {
  const titles = (json.displayName ?? '').split(SEPARATOR);
  return { id: json.id, title: titles[titles.length - 1] ?? `#${json.id}`, parents: titles.slice(0, -1) };
}

export const fullPath = (task: TaskRef) => [...task.parents, task.title].join(' › ');

/** The title and its parent: enough to recognize the task in a row. */
export const shortPath = (task: TaskRef) =>
  task.parents.length === 0 ? task.title : `${task.parents[task.parents.length - 1]} › ${task.title}`;

/** A cost unit (Kost2) of a task. */
export interface Kost2Ref {
  id: number;
  label: string;
}

/** A cost unit of a booking: number and description, else its display name (the rows of `appList`). */
export function kost2OfTimesheet(kost2: TimesheetKost2): Kost2Ref {
  const number = kost2.formattedNumber;
  const label = number ? (kost2.description ? `${number} ${kost2.description}` : number) : (kost2.displayName ?? '');
  return { id: kost2.id, label };
}

/** A cost unit of `task/info`: the whole text as title (`5.123.45.01: <Kost2Art> - <project>`). */
export const kost2OfTaskInfo = (kost2: TaskKost2): Kost2Ref => ({ id: kost2.id, label: kost2.title ?? '' });

/** The number of the label (`5.123.45.01`), the whole label if it has no text. */
export function kost2Number(kost2: Kost2Ref): string {
  return kost2.label.split(/:?\s/)[0];
}

/** The text of the label after the number, empty if there is none. */
export function kost2Text(kost2: Kost2Ref): string {
  const match = /:?\s/.exec(kost2.label);
  return match ? kost2.label.substring(match.index + match[0].length) : '';
}

/** What was done, without the times: the part a recent entry, a template or a stopped timer fills in. */
export interface BookingWhat {
  task: TaskRef | null;
  kost2: Kost2Ref | null;
  description: string;
  location: string;
  reference: string;
  tag: string | null;
}

export const EMPTY_WHAT: BookingWhat = { task: null, kost2: null, description: '', location: '', reference: '', tag: null };

export function whatOfTimesheet(json: TimesheetData): BookingWhat {
  return {
    task: json.task ? taskOfTimesheet(json.task) : null,
    kost2: json.kost2 ? kost2OfTimesheet(json.kost2) : null,
    description: json.description ?? '',
    location: json.location ?? '',
    reference: json.reference ?? '',
    tag: json.tag || null,
  };
}

/** Two entries that book the same: shown once in the recent list. */
export function whatKey(what: BookingWhat): string {
  return [what.task?.id, what.kost2?.id, what.description, what.location, what.reference, what.tag].join('|');
}

/** A booking of the user, with the json of the server it came as. */
export interface Timesheet {
  id: number;
  json: TimesheetData;
  what: BookingWhat;
  start: Date;
  stop: Date;
}

export function timesheetOf(json: TimesheetData): Timesheet {
  return {
    id: json.id ?? 0,
    json,
    what: whatOfTimesheet(json),
    start: new Date(json.startTime ?? 0),
    stop: new Date(json.stopTime ?? 0),
  };
}

export const durationOf = (it: { start: Date; stop: Date }) => minutesBetween(it.start, it.stop);

/** Booked by a proposed grant and not yet accepted by the project management. */
export const acceptancePending = (sheet: Timesheet) => sheet.json.acceptance != null;

/** A free time between two bookings of a day. */
export interface Gap {
  start: Date;
  stop: Date;
}

/** The bookings of a week (seven days from the user's first day of the week) and the tags the installation offers. */
export interface TimesheetWeek {
  weekStart: Date;
  sheets: Timesheet[];
  tags: string[];
  /** The daily targets in minutes by day (`yyyy-MM-dd`), empty from an older server (see targetOf). */
  targetMinutes: Record<string, number>;
  /** The server wasn't reachable: the week as it was loaded last. */
  fromCache: boolean;
}

export function weekOf(weekStart: Date, json: AppTimesheetList, fromCache = false): TimesheetWeek {
  return {
    weekStart,
    sheets: (json.timesheets ?? []).map(timesheetOf),
    tags: (json.tags ?? []).map(String),
    targetMinutes: json.targetMinutes ?? {},
    fromCache,
  };
}

/**
 * The minutes to book on [day]: a fifth of the weekly working hours valid on it (sent by the server), 8 hours from an
 * older server or a week cached before.
 */
export function targetOf(week: TimesheetWeek | null | undefined, day: Date): number {
  const key = `${day.getFullYear()}-${two(day.getMonth() + 1)}-${two(day.getDate())}`;
  return week?.targetMinutes[key] ?? TARGET_MINUTES_PER_DAY;
}

export function bookingsOf(week: TimesheetWeek, day: Date): Timesheet[] {
  return week.sheets.filter((it) => sameDay(it.start, day)).sort((a, b) => a.start.getTime() - b.start.getTime());
}

export function totalOf(sheets: Timesheet[]): number {
  return sheets.reduce((sum, it) => sum + durationOf(it), 0);
}

/**
 * The gaps between the bookings of a day, and today from the last one up to now (at most 20:00, so the app opened late
 * in the evening doesn't offer the night).
 */
export function gapsOf(week: TimesheetWeek, day: Date, now: Date): Gap[] {
  const list = bookingsOf(week, day);
  const gaps: Gap[] = [];
  for (let i = 0; i + 1 < list.length; i++) {
    if (minutesBetween(list[i].stop, list[i + 1].start) >= BOOKING_STEP_MINUTES) {
      gaps.push({ start: list[i].stop, stop: list[i + 1].start });
    }
  }
  const last = list[list.length - 1];
  if (last && sameDay(day, now)) {
    let until = floor5(now);
    const evening = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 20);
    if (until > evening) until = evening;
    if (minutesBetween(last.stop, until) >= BOOKING_STEP_MINUTES) gaps.push({ start: last.stop, stop: until });
  }
  return gaps;
}

/** The start of a new booking: the end of the last booking of the day, or the start of the working day. */
export function nextStartOf(week: TimesheetWeek | null | undefined, day: Date): Date {
  const list = week ? bookingsOf(week, day) : [];
  const last = list[list.length - 1];
  return last ? last.stop : addMinutes(dayOf(day), DEFAULT_FIRST_HOUR * 60);
}

/** A template of the user. */
export interface TimesheetTemplate {
  id: number;
  name: string;
  taskId: number | null;
  kost2Id: number | null;
  description: string;
  location: string;
  reference: string;
  tag: string | null;
}

export function templateOf(json: TimesheetFavorite): TimesheetTemplate {
  return {
    id: json.id,
    name: json.name ?? '',
    taskId: json.taskId ?? null,
    kost2Id: json.cost2Id ?? null,
    description: json.description ?? '',
    location: json.location ?? '',
    reference: json.reference ?? '',
    tag: json.tag || null,
  };
}

/** A task with the cost units a booking on it may charge. */
export interface TaskInfo {
  task: TaskRef;
  kost2List: Kost2Ref[];
  /** The ids of the ancestors, from the top, as the task's parents has their titles. */
  pathIds: number[];
}

export function taskInfoOf(json: TaskInfoData): TaskInfo {
  const path = json.path ?? [];
  return {
    task: {
      id: json.id,
      title: json.title ?? `#${json.id}`,
      parents: path.filter((it) => it.title != null).map((it) => String(it.title)),
    },
    kost2List: (json.kost2List ?? []).map(kost2OfTaskInfo),
    pathIds: path.map((it) => it.id),
  };
}

/** What the booking form saves. */
export interface BookingDraft extends BookingWhat {
  task: TaskRef;
  start: Date;
  stop: Date;
}

/** A running timer, kept on the device only. */
export interface RunningTimer {
  start: Date;
  what: BookingWhat | null;
}

/** The timer as stored in its file. */
export interface StoredTimer {
  start: number;
  what: BookingWhat | null;
}

export const storedTimer = (timer: RunningTimer): StoredTimer => ({ start: timer.start.getTime(), what: timer.what });

export const timerOf = (stored: StoredTimer): RunningTimer => ({ start: new Date(stored.start), what: stored.what });
