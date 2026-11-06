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

import { ApiException } from '../../../core/api/apiException';
import { TimesheetsRepository } from '../timesheetsRepository';
import { sheet } from '../../../testing/timesheetFixtures';

function memoryCache() {
  const entries: Record<string, unknown> = {};
  return {
    entries,
    read: async <T,>(name: string) => (entries[name] as T) ?? null,
    write: (name: string, json: unknown) => void (entries[name] = JSON.parse(JSON.stringify(json))),
    delete: (name: string) => void delete entries[name],
  };
}

function fakeApi(answer: (method: string, path: string, body?: unknown) => unknown) {
  const calls: { method: string; path: string; body?: unknown }[] = [];
  const call = (method: string) => async (path: string, options?: { body?: unknown }) => {
    calls.push({ method, path, body: options?.body });
    return answer(method, path, options?.body);
  };
  return { calls, api: { get: call('GET'), post: call('POST'), put: call('PUT'), delete: call('DELETE') } as never };
}

const monday = new Date(2031, 2, 10);

describe('TimesheetsRepository', () => {
  it('loads a week and keeps it for reading offline', async () => {
    const cache = memoryCache();
    let offline = false;
    const { calls, api } = fakeApi(() => {
      if (offline) throw new TypeError('Network request failed');
      return { timesheets: [sheet(1, '8:00', '9:00')], tags: ['Remote'] };
    });
    const repository = new TimesheetsRepository(api, cache, 42);
    const week = await repository.week(monday);
    expect(week.sheets.map((it) => it.id)).toEqual([1]);
    expect(week.fromCache).toBe(false);
    expect(calls[0].path).toBe(`/rs/timesheet/appList?from=${monday.getTime()}&to=${new Date(2031, 2, 17).getTime()}`);
    offline = true;
    const cached = await repository.week(monday);
    expect(cached.fromCache).toBe(true);
    expect(cached.tags).toEqual(['Remote']);
    await expect(repository.week(new Date(2031, 2, 17))).rejects.toThrow(TypeError);
  });

  it('inserts a new booking for the user', async () => {
    const { calls, api } = fakeApi(() => undefined);
    await new TimesheetsRepository(api, memoryCache(), 42).save({
      task: { id: 3, title: 'Dev', parents: [] },
      kost2: { id: 7, label: '5.020.01.02' },
      start: new Date(2031, 2, 10, 8),
      stop: new Date(2031, 2, 10, 9),
      description: 'Tests',
      location: '',
      reference: '',
      tag: 'Remote',
    });
    expect(calls[0].method).toBe('PUT');
    expect(calls[0].path).toBe('/rs/timesheet/insert');
    const data = (calls[0].body as { data: Record<string, unknown> }).data;
    expect(data.task).toEqual({ id: 3 });
    expect(data.kost2).toEqual({ id: 7 });
    expect(data.user).toEqual({ id: 42 });
    expect(data.startTime).toBe(new Date(2031, 2, 10, 8).toISOString());
    expect(data.tag).toBe('Remote');
    expect('id' in data).toBe(false);
  });

  it('updates from the whole booking, so unknown fields survive', async () => {
    const { calls, api } = fakeApi((method) => (method === 'GET' ? { ...sheet(5, '8:00', '9:00'), timeSavedByAI: 25 } : undefined));
    await new TimesheetsRepository(api, memoryCache(), 42).save(
      {
        task: { id: 3, title: 'Dev', parents: [] },
        kost2: null,
        start: new Date(2031, 2, 10, 8),
        stop: new Date(2031, 2, 10, 10),
        description: 'Changed',
        location: '',
        reference: '',
        tag: null,
      },
      5,
    );
    expect(calls.map((it) => `${it.method} ${it.path}`)).toEqual(['GET /rs/timesheet/5', 'PUT /rs/timesheet/update']);
    const data = (calls[1].body as { data: Record<string, unknown> }).data;
    expect(data.id).toBe(5);
    expect(data.timeSavedByAI).toBe(25);
    expect(data.description).toBe('Changed');
    expect(data.kost2).toBeNull();
    expect('user' in data).toBe(false);
  });

  it('passes the refusals of the server on with their fields', async () => {
    const { api } = fakeApi(() => {
      throw new ApiException('refused', 406, [{ fieldId: 'kost2', message: 'Cost unit' }, { message: 'Overlap' }]);
    });
    const error = await new TimesheetsRepository(api, memoryCache(), 42)
      .save({ task: { id: 3, title: 'Dev', parents: [] }, kost2: null, start: new Date(), stop: new Date(), description: '', location: '', reference: '', tag: null })
      .then(
        () => null,
        (e: ApiException) => e,
      );
    expect(error?.statusCode).toBe(406);
    expect(error?.validationErrors.map((it) => it.fieldId)).toEqual(['kost2', undefined]);
  });

  it('restores a deleted booking with what it was', async () => {
    const { calls, api } = fakeApi((method) => (method === 'GET' ? sheet(5, '8:00', '9:00') : undefined));
    const repository = new TimesheetsRepository(api, memoryCache(), 42);
    const data = await repository.delete(5);
    expect(calls.at(-1)).toMatchObject({ method: 'DELETE', path: '/rs/timesheet/markAsDeleted' });
    await repository.undelete(data);
    expect(calls.at(-1)).toMatchObject({ method: 'PUT', path: '/rs/timesheet/undelete', body: { data: { id: 5 } } });
  });

  it('lists recent bookings once, resolves the task and cost unit of a template', async () => {
    const { api } = fakeApi((_, path) => {
      if (path.endsWith('/recentList')) {
        return {
          timesheets: [
            { ...sheet(1, '8:00', '9:00'), description: 'A' },
            { ...sheet(2, '9:00', '10:00'), description: 'A' },
            { ...sheet(3, '10:00', '11:00'), description: 'B' },
            { description: 'without task' },
          ],
        };
      }
      return {
        id: 3,
        title: 'Dev',
        path: [{ id: 1, title: 'ProjectForge' }],
        kost2List: [
          { id: 7, title: 'first' },
          { id: 8, title: 'second' },
        ],
      };
    });
    const repository = new TimesheetsRepository(api, memoryCache(), 42);
    expect((await repository.recent()).map((it) => it.description)).toEqual(['A', 'B']);
    const what = await repository.whatOf({
      id: 1,
      name: 'T',
      taskId: 3,
      kost2Id: 8,
      description: 'D',
      location: '',
      reference: '',
      tag: null,
    });
    expect(what.task?.parents).toEqual(['ProjectForge']);
    expect(what.kost2?.label).toBe('second');
    expect(what.description).toBe('D');
  });

  it('keeps the running timer in a file', async () => {
    const repository = new TimesheetsRepository(fakeApi(() => undefined).api, memoryCache(), 42);
    expect(await repository.loadTimer()).toBeNull();
    repository.saveTimer({ start: new Date(2031, 2, 10, 8, 3), what: null });
    expect((await repository.loadTimer())?.start).toEqual(new Date(2031, 2, 10, 8, 3));
    repository.clearTimer();
    expect(await repository.loadTimer()).toBeNull();
  });
});
