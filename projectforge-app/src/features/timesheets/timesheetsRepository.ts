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

import { addDays, two } from '@projectforge/shared/booking-time';
import type {
  AppTimesheetList,
  DisplayObject,
  NewTimesheetFavorite,
  PostData,
  RecentTimesheets,
  TaskChild,
  TaskInfo as TaskInfoData,
  TimesheetData,
  TimesheetFavorite,
  TimesheetSave,
} from '@projectforge/shared/rs/timesheet';
import type { TimesheetTimerInfo, TimesheetTimerStartRequest } from '@projectforge/shared/rs/timesheet-timer';

import type { ApiClient } from '../../core/api/apiClient';
import { ApiException } from '../../core/api/apiException';
import { withQuery } from '../../core/api/query';
import {
  BookingDraft,
  BookingWhat,
  RunningTimer,
  StoredTimer,
  storedTimer,
  TaskInfo,
  taskInfoOf,
  taskOfDisplayObject,
  TaskRef,
  templateOf,
  TimesheetTemplate,
  timerOf,
  TimesheetWeek,
  weekOf,
  whatKey,
  whatOfTimesheet,
} from './timesheet';

export type { TaskChild } from '@projectforge/shared/rs/timesheet';

/** The part of JsonCache the repository uses. */
export interface SheetsCache {
  read<T>(name: string): Promise<T | null>;
  write(name: string, json: unknown): void;
  delete(name: string): void;
}

const isOffline = (error: unknown) => !(error instanceof ApiException) || error.statusCode == null;

/**
 * The time sheets of the user (TimesheetEntityRest `/rs/timesheet`) and the tasks to book on (TaskServicesRest
 * `/rs/task`). Saving needs the server: its checks (overlap, closed tasks, cost units, protected periods) can't be
 * repeated on the device. Only the weeks seen last are kept for reading without network. Port of the Flutter app's
 * TimesheetsRepository.
 */
export class TimesheetsRepository {
  private static readonly PATH = '/rs/timesheet';
  private static readonly TASK_PATH = '/rs/task';
  private static readonly TIMER_NAME = 'timesheetTimer';
  private static readonly TIMER_PATH = '/rs/timesheetTimer';

  constructor(
    private readonly api: Pick<ApiClient, 'get' | 'post' | 'put' | 'delete'>,
    private readonly cache: SheetsCache,
    readonly userId: number,
  ) {}

  private static weekName(weekStart: Date) {
    return `timesheetWeek_${weekStart.getFullYear()}${two(weekStart.getMonth() + 1)}${two(weekStart.getDate())}`;
  }

  /**
   * The bookings of the week starting on [weekStart], with the tags. Without a connection the week as it was loaded last
   * (marked as such), if there is one.
   */
  async week(weekStart: Date): Promise<TimesheetWeek> {
    const name = TimesheetsRepository.weekName(weekStart);
    try {
      const json = await this.api.get<AppTimesheetList>(
        withQuery(`${TimesheetsRepository.PATH}/appList`, {
          from: weekStart.getTime(),
          to: addDays(weekStart, 7).getTime(),
        }),
      );
      this.cache.write(name, json);
      return weekOf(weekStart, json);
    } catch (e) {
      const cached = isOffline(e) ? await this.cache.read<AppTimesheetList>(name) : null;
      if (cached == null) throw e;
      return weekOf(weekStart, cached, true);
    }
  }

  /** The whole booking as the server has it (the list rows are leaner). */
  load(id: number): Promise<TimesheetData & Record<string, unknown>> {
    return this.api.get(`${TimesheetsRepository.PATH}/${id}`);
  }

  /**
   * Books [draft] as a new sheet, or as [id] if given. Throws the ApiException with the validation errors of the
   * server (field `task`, `kost2`, or the period) if it refuses.
   */
  async save(draft: BookingDraft, id?: number | null): Promise<void> {
    // An edit starts from the whole sheet, so what this app doesn't show (AI time savings, …) isn't lost.
    const stored = id == null ? {} : await this.load(id);
    const data: TimesheetSave & Record<string, unknown> = {
      ...stored,
      task: { id: draft.task.id },
      kost2: draft.kost2 == null ? null : { id: draft.kost2.id },
      startTime: draft.start.toISOString(),
      stopTime: draft.stop.toISOString(),
      description: draft.description,
      location: draft.location,
      reference: draft.reference,
      tag: draft.tag,
    };
    if (id == null) data.user = { id: this.userId };
    const body: PostData<typeof data> = { data };
    await this.api.put(`${TimesheetsRepository.PATH}/${id == null ? 'insert' : 'update'}`, { body });
  }

  /** Marks the booking as deleted. Returns it as it was, for [undelete]. */
  async delete(id: number): Promise<TimesheetData> {
    const data = await this.load(id);
    const body: PostData<TimesheetData> = { data };
    await this.api.delete(`${TimesheetsRepository.PATH}/markAsDeleted`, { body });
    return data;
  }

  async undelete(data: TimesheetData): Promise<void> {
    const body: PostData<TimesheetData> = { data };
    await this.api.put(`${TimesheetsRepository.PATH}/undelete`, { body });
  }

  /** The bookings last made, newest first, without the same twice (what was booked, not when). */
  async recent(): Promise<BookingWhat[]> {
    const json = await this.api.get<RecentTimesheets>(`${TimesheetsRepository.PATH}/recentList`);
    const seen = new Set<string>();
    return (json.timesheets ?? []).map(whatOfTimesheet).filter((what) => {
      if (what.task == null) return false;
      const key = whatKey(what);
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  }

  async templates(): Promise<TimesheetTemplate[]> {
    const list = await this.api.get<TimesheetFavorite[]>(`${TimesheetsRepository.PATH}/favorites/list`);
    return list.map(templateOf);
  }

  async createTemplate(name: string, what: BookingWhat & { task: TaskRef }): Promise<void> {
    const body: NewTimesheetFavorite = {
      name,
      timesheet: {
        task: { id: what.task.id },
        kost2: what.kost2 == null ? null : { id: what.kost2.id },
        description: what.description,
        location: what.location,
        reference: what.reference,
        tag: what.tag,
      },
    };
    await this.api.post(`${TimesheetsRepository.PATH}/favorites/create`, { body });
  }

  async deleteTemplate(id: number): Promise<void> {
    await this.api.post(withQuery(`${TimesheetsRepository.PATH}/favorites/delete`, { id }));
  }

  /** What a template books: its task (with the path) and cost unit. */
  async whatOf(template: TimesheetTemplate): Promise<BookingWhat> {
    const info = template.taskId == null ? null : await this.taskInfo(template.taskId);
    return {
      task: info?.task ?? null,
      kost2: info?.kost2List.find((it) => it.id === template.kost2Id) ?? null,
      description: template.description,
      location: template.location,
      reference: template.reference,
      tag: template.tag,
    };
  }

  /**
   * The bookable tasks matching [search] (words of the path), the ones of the user's booking grants first. With
   * [parentId] only that task and the ones below it.
   */
  async searchTasks(search: string, parentId?: number | null): Promise<TaskRef[]> {
    const list = await this.api.get<DisplayObject[]>(
      withQuery(`${TimesheetsRepository.TASK_PATH}/tree/autosearch`, {
        search,
        maxResults: 40,
        onlyBookable: true,
        parentId,
      }),
    );
    return list.map(taskOfDisplayObject);
  }

  /** The tasks picked last, newest first. */
  async recentTasks(): Promise<TaskRef[]> {
    const list = await this.api.get<DisplayObject[]>(`${TimesheetsRepository.TASK_PATH}/recent/list`);
    return list.map(taskOfDisplayObject);
  }

  /** The visible children of [parentId] (of the root if null), for browsing the task tree. */
  taskChildren(parentId: number | null): Promise<TaskChild[]> {
    return this.api.get<TaskChild[]>(withQuery(`${TimesheetsRepository.TASK_PATH}/tree/children`, { parentId }));
  }

  async taskInfo(id: number): Promise<TaskInfo> {
    return taskInfoOf(await this.api.get<TaskInfoData>(`${TimesheetsRepository.TASK_PATH}/info/${id}`));
  }

  /** The running timer, null if none. */
  async loadTimer(): Promise<RunningTimer | null> {
    const stored = await this.cache.read<StoredTimer>(TimesheetsRepository.TIMER_NAME);
    return stored == null ? null : timerOf(stored);
  }

  /**
   * The timer of the user at the server (started in the app or on the web pages), null without one. Throws if there is
   * no answer (offline, an older server without the endpoint): the timer on the device stays then.
   */
  async serverTimer(): Promise<RunningTimer | null> {
    const info = await this.api.get<TimesheetTimerInfo>(TimesheetsRepository.TIMER_PATH, { renewLogin: false });
    if (!info.running || info.startedAt == null) return null;
    // The server's clock gives the start, the device's clock is only used for showing the elapsed time.
    return { start: new Date(info.startedAt), what: await this.whatOfTimer(info), remote: true };
  }

  /** Starts the timer at the server; the answer has its start. Throws like [serverTimer]. */
  async startServerTimer(what: BookingWhat | null): Promise<RunningTimer> {
    const body: TimesheetTimerStartRequest = {
      taskId: what?.task?.id ?? null,
      kost2Id: what?.kost2?.id ?? null,
      description: what?.description || null,
      location: what?.location || null,
      reference: what?.reference || null,
      tag: what?.tag ?? null,
    };
    const info = await this.api.post<TimesheetTimerInfo>(`${TimesheetsRepository.TIMER_PATH}/start`, { body });
    return { start: new Date(info.startedAt ?? Date.now()), what, remote: true };
  }

  async clearServerTimer(): Promise<void> {
    await this.api.post(`${TimesheetsRepository.TIMER_PATH}/clear`);
  }

  /** What the timer books on: its task and cost unit are looked up by their ids, without any a timer books nothing. */
  private async whatOfTimer(info: TimesheetTimerInfo): Promise<BookingWhat | null> {
    const texts = {
      description: info.description ?? '',
      location: info.location ?? '',
      reference: info.reference ?? '',
      tag: info.tag || null,
    };
    if (info.taskId == null) {
      return texts.description || texts.location || texts.reference || texts.tag ? { task: null, kost2: null, ...texts } : null;
    }
    try {
      const taskInfo = await this.taskInfo(info.taskId);
      const kost2 = taskInfo.kost2List.find((it) => it.id === info.kost2Id) ?? null;
      return { task: taskInfo.task, kost2, ...texts };
    } catch {
      // The timer is shown without its task (it is chosen when stopping) rather than not at all.
      return { task: null, kost2: null, ...texts };
    }
  }

  saveTimer(timer: RunningTimer): void {
    this.cache.write(TimesheetsRepository.TIMER_NAME, storedTimer(timer));
  }

  clearTimer(): void {
    this.cache.delete(TimesheetsRepository.TIMER_NAME);
  }
}
