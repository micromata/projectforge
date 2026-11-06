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

import { useEffect, useSyncExternalStore } from 'react';

import type { ApiClient } from '../../core/api/apiClient';
import { authController } from '../../core/auth/authController';
import { JsonCache } from '../../core/cache/jsonCache';
import type { BookingWhat, RunningTimer, TaskRef, TimesheetWeek } from './timesheet';
import { TimesheetsRepository } from './timesheetsRepository';

export type WeekState = { status: 'loading' } | { status: 'error'; error: unknown } | { status: 'data'; week: TimesheetWeek };

export interface TimesheetsState {
  /** By the time of the Monday. */
  weeks: Record<number, WeekState>;
  timer: RunningTimer | null;
  /** What the user booked last, for the booking form (loaded again whenever it is opened). */
  recent: BookingWhat[];
}

const EMPTY: TimesheetsState = { weeks: {}, timer: null, recent: [] };

/** What the booking form starts with: an existing booking ([id]), a gap, a duplicate, a stopped timer or a new one. */
export interface BookingStart {
  id?: number;
  what?: BookingWhat | null;
  start: Date;
  stop: Date;
  /** Started by stopping the timer: it ends when the booking is saved. */
  fromTimer?: boolean;
}

/**
 * The weeks of time sheets of the active login, the running timer (kept on the device, so it survives closing the app)
 * and the recent bookings. Port of the Flutter app's timesheets controllers.
 */
class TimesheetsStore {
  private state: TimesheetsState = EMPTY;
  private api: ApiClient | null = null;
  private repo: TimesheetsRepository | null = null;
  private listeners = new Set<() => void>();
  /** The booking form asked for on a phone (a screen of its own), taken once by it. */
  private pendingBooking: BookingStart | null = null;
  /** The task picker's answer and what it starts with, see [pickTask]. */
  private taskPick: ((task: TaskRef | null) => void) | null = null;
  private taskPickArgs: { recentFromBookings: TaskRef[]; current: TaskRef | null } = { recentFromBookings: [], current: null };

  /** Deletes a booking with an undo (the screen of the time sheets, which has the list). */
  private deleteHandler: ((id: number) => void) | null = null;

  getState = () => this.state;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  /** The repository of the active login, null while logged out. */
  repository(): TimesheetsRepository | null {
    this.ensureLogin();
    return this.repo;
  }

  /** Starts anew for another login (another server, another user): nothing of the former one stays. */
  ensureLogin() {
    const api = authController.api;
    if (api === this.api) return;
    this.api = api;
    const session = authController.getState().session;
    this.repo = api && session ? new TimesheetsRepository(api, JsonCache, session.userId) : null;
    this.state = EMPTY;
    this.emit();
    const repo = this.repo;
    if (repo) void repo.loadTimer().then((timer) => this.repo === repo && this.set({ timer }));
  }

  /** Loads the week of [monday] if it isn't loaded yet ([force]: again, the shown one stays until the answer). */
  loadWeek(monday: Date, force = false) {
    const repo = this.repository();
    if (repo == null) return;
    const key = monday.getTime();
    const current = this.state.weeks[key];
    if (current != null && !force) return;
    if (current == null || current.status === 'error') this.setWeek(key, { status: 'loading' });
    void repo.week(monday).then(
      (week) => this.repo === repo && this.setWeek(key, { status: 'data', week }),
      (error: unknown) => {
        if (this.repo !== repo) return;
        // A shown week stays, the error is only shown without one.
        if (this.state.weeks[key]?.status !== 'data') this.setWeek(key, { status: 'error', error });
      },
    );
  }

  /** Loads the shown weeks again (after a booking, a deletion, on opening the tab). */
  reloadWeeks() {
    for (const key of Object.keys(this.state.weeks)) this.loadWeek(new Date(Number(key)), true);
  }

  loadRecent() {
    const repo = this.repository();
    void repo
      ?.recent()
      .then((recent) => this.repo === repo && this.set({ recent }))
      .catch(() => undefined);
  }

  startTimer(what: BookingWhat | null = null) {
    const timer = { start: new Date(), what };
    this.repository()?.saveTimer(timer);
    this.set({ timer });
  }

  stopTimer() {
    this.repository()?.clearTimer();
    this.set({ timer: null });
  }

  setDeleteHandler(handler: ((id: number) => void) | null) {
    this.deleteHandler = handler;
  }

  /** The booking form (a screen of its own) asks to delete its booking. */
  requestDelete(id: number) {
    this.deleteHandler?.(id);
  }

  /** Hands the booking form its start (on a phone a screen of its own, which takes it). */
  requestBooking(start: BookingStart) {
    this.pendingBooking = start;
  }

  takeBooking(): BookingStart | null {
    const start = this.pendingBooking;
    this.pendingBooking = null;
    return start;
  }

  /** Waits for the task picker (a screen of its own): the chosen task, null if it was left without one. */
  pickTask(recentFromBookings: TaskRef[], current: TaskRef | null): Promise<TaskRef | null> {
    this.taskPick?.(null);
    this.taskPickArgs = { recentFromBookings, current };
    return new Promise((resolve) => (this.taskPick = resolve));
  }

  /** What the task picker starts with. */
  taskPickerArgs() {
    return this.taskPickArgs;
  }

  /** The answer of the task picker. */
  taskPicked(task: TaskRef | null) {
    const resolve = this.taskPick;
    this.taskPick = null;
    resolve?.(task);
  }

  private setWeek(key: number, week: WeekState) {
    this.set({ weeks: { ...this.state.weeks, [key]: week } });
  }

  private set(change: Partial<TimesheetsState>) {
    this.state = { ...this.state, ...change };
    this.emit();
  }

  private emit() {
    this.listeners.forEach((it) => it());
  }
}

export const timesheets = new TimesheetsStore();

/** The weeks, the timer and the recent bookings of the active login. */
export function useTimesheets(): TimesheetsState {
  useEffect(() => timesheets.ensureLogin());
  return useSyncExternalStore(timesheets.subscribe, timesheets.getState);
}
