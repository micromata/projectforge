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


// Port of projectforge-flutter/test/features/timesheets/timesheets_screen_test.dart: the day list and the booking form
// against a fake server (the screen of a phone shows the form as a route of its own).
import { dayOf, mondayOf } from '@projectforge/shared/booking-time';
import { act, fireEvent, render, screen } from '@testing-library/react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { fakeHttp, FakeAnswer, RecordedRequest } from '../../../testing/fakeHttp';
import { ApiClient } from '../../../core/api/apiClient';
import { t } from '../../../core/i18n/translations';
import { BookingForm } from '../BookingForm';
import { DayActions, DayList } from '../DayList';
import { timesheets } from '../timesheetsStore';

// The icons need the fonts of expo-asset, not there in the tests.
jest.mock('@expo/vector-icons', () => ({ Ionicons: () => null }));
// The swipes need the native worklets: the rows without them.
jest.mock('react-native-gesture-handler/ReanimatedSwipeable', () => ({
  __esModule: true,
  default: ({ children }: { children: unknown }) => children,
}));

const mockApi: { current: ApiClient | null } = { current: null };
jest.mock('../../../core/auth/authController', () => ({
  authController: {
    get api() {
      return mockApi.current;
    },
    getState: () => ({ session: { userId: 1 } }),
  },
}));
jest.mock('../../../core/cache/jsonCache', () => ({
  JsonCache: { read: async () => null, write: () => undefined, delete: () => undefined, clearAll: () => undefined },
}));

const at = (hour: number) => {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate(), hour);
};
const TASK = { id: 3, title: 'Development', path: 'ProjectForge -> Next' };
const sheet = (id: number, from: number, to: number, description: string) => ({
  id,
  task: TASK,
  description,
  startTime: at(from).toISOString(),
  stopTime: at(to).toISOString(),
});

function server(put?: (request: RecordedRequest) => FakeAnswer) {
  const { http, requests } = fakeHttp((request) => {
    const path = new URL(request.url).pathname;
    const method = request.init.method ?? 'GET';
    if (method === 'PUT') return put?.(request) ?? { body: {} };
    if (path === '/rs/timesheet/appList') {
      return { body: { timesheets: [sheet(1, 8, 10, 'Morning work'), sheet(2, 11, 12, 'Before lunch')], tags: [] } };
    }
    if (path === '/rs/timesheet/recentList') return { body: { timesheets: [{ task: TASK, description: 'Recent work' }] } };
    if (path === '/rs/timesheet/favorites/list') return { body: [] };
    if (path === '/rs/task/info/3') return { body: { id: 3, title: 'Development', kost2List: [] } };
    return { status: 404, body: { message: `${method} ${path}` } };
  });
  mockApi.current = new ApiClient('https://pf.example.com', {
    http,
    appVersion: '1',
    token: () => 't',
    proxyCookies: () => ({}),
    onUnauthorized: () => undefined,
    onReauthenticate: async () => false,
  });
  timesheets.ensureLogin();
  return requests;
}

const actions = (book: DayActions['book'] = () => undefined): DayActions => ({
  open: () => undefined,
  book,
  duplicate: () => undefined,
  delete: () => undefined,
  menu: () => undefined,
  refresh: async () => undefined,
  retry: () => undefined,
});

const gapText = () => t('app.timesheet.gap', '10:00', '11:00', '1:00');

async function renderForm(start = at(10), stop = at(11), onDone = jest.fn()) {
  timesheets.loadRecent();
  await render(
    <SafeAreaProvider initialMetrics={{ frame: { x: 0, y: 0, width: 390, height: 844 }, insets: { top: 0, left: 0, right: 0, bottom: 0 } }}>
      <BookingForm initial={{ start, stop }} tags={[]} onDone={onDone} onPickTask={async () => null} />
    </SafeAreaProvider>,
  );
  await screen.findByText('Recent work');
  return onDone;
}

test('the day shows its bookings, the total and the gap between them; a tap on the gap books exactly that time', async () => {
  server();
  const today = dayOf(new Date());
  await act(() => timesheets.loadWeek(mondayOf(today), true));
  const book = jest.fn();
  await render(
    <DayList day={today} week={timesheets.getState().weeks[mondayOf(today).getTime()]} hidden={new Set()} actions={actions(book)} />,
  );
  expect(screen.getByText('Morning work')).toBeTruthy();
  expect(screen.getByText('Before lunch')).toBeTruthy();
  expect(screen.getByText('Σ 3:00 h')).toBeTruthy();
  await fireEvent.press(screen.getByText(gapText()));
  expect(book).toHaveBeenCalledWith(at(10), at(11));
});

test('a booking with a recent entry sends that time', async () => {
  const requests = server();
  const onDone = await renderForm();
  await fireEvent.press(screen.getByText('Recent work'));
  await act(async () => await fireEvent.press(screen.getByText(t('app.timesheet.bookWithDuration', '1:00'))));
  const insert = requests.find((it) => it.url.endsWith('/rs/timesheet/insert'));
  const data = (insert?.body as { data: Record<string, unknown> }).data;
  expect(data.task).toEqual({ id: 3 });
  expect(data.description).toBe('Recent work');
  expect(data.startTime).toBe(at(10).toISOString());
  expect(data.stopTime).toBe(at(11).toISOString());
  expect(onDone).toHaveBeenCalled();
});

test('without a task nothing is sent', async () => {
  const requests = server();
  const onDone = await renderForm();
  await act(async () => await fireEvent.press(screen.getByText(t('app.timesheet.bookWithDuration', '1:00'))));
  expect(screen.getByText(t('app.timesheet.task.required'))).toBeTruthy();
  expect(requests.filter((it) => it.init.method === 'PUT')).toHaveLength(0);
  expect(onDone).not.toHaveBeenCalled();
});

test('the refusal of the server is shown at the time, the form stays open', async () => {
  server(() => ({ status: 406, body: { validationErrors: [{ message: 'Overlap with 08:00-10:00.' }] } }));
  const onDone = await renderForm();
  await fireEvent.press(screen.getByText('Recent work'));
  await act(async () => await fireEvent.press(screen.getByText(t('app.timesheet.bookWithDuration', '1:00'))));
  expect(await screen.findByText('Overlap with 08:00-10:00.')).toBeTruthy();
  expect(onDone).not.toHaveBeenCalled();
});
