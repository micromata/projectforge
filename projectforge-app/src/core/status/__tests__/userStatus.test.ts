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

// Port of the earlier Flutter app's user_status_controller_test and theme_controller_test.
import type { UserStatusResult } from '@projectforge/shared/rs/user-status';

import { fakeHttp, FakeAnswer, RecordedRequest } from '../../../testing/fakeHttp';
import { ApiClient } from '../../api/apiClient';
import { alertMessage, chatEnabled, parseTheme, UserStatusStore } from '../userStatus';

const mockStored = new Map<string, unknown>();
jest.mock('../../cache/jsonCache', () => ({
  JsonCache: {
    read: async (name: string) => mockStored.get(name) ?? null,
    write: (name: string, json: unknown) => mockStored.set(name, json),
  },
}));

beforeEach(() => mockStored.clear());

function api(handler: (request: RecordedRequest) => FakeAnswer) {
  const { http, requests } = fakeHttp(handler);
  const client = new ApiClient('https://pf.example.com', {
    http,
    appVersion: '1',
    token: () => 'token',
    proxyCookies: () => ({}),
    onUnauthorized: () => undefined,
    onReauthenticate: async () => false,
  });
  return { client, requests };
}

/** A store started for [client], stopped after the test (its timer). */
async function started(client: ApiClient) {
  const store = new UserStatusStore();
  await store.start(client);
  // The fallback of an older server is asked for without waiting.
  await new Promise((resolve) => setTimeout(resolve, 0));
  stores.push(store);
  return store;
}
const stores: UserStatusStore[] = [];
afterEach(() => stores.splice(0).forEach((it) => it.stop()));

test('unread count and switched off chat', () => {
  expect(chatEnabled({} as UserStatusResult)).toBe(true);
  expect(chatEnabled(null)).toBe(true);
  expect(chatEnabled({ disabledFeatures: ['CALENDAR'] } as UserStatusResult)).toBe(true);
  expect(chatEnabled({ disabledFeatures: ['CHAT'] } as UserStatusResult)).toBe(false);
});

test('message of the administrators', () => {
  expect(alertMessage({ alertMessage: ' Maintenance ' } as UserStatusResult)).toBe('Maintenance');
  expect(alertMessage({ alertMessage: ' ' } as UserStatusResult)).toBeNull();
  expect(alertMessage({} as UserStatusResult)).toBeNull();
});

test('offline the last status stays', async () => {
  let offline = false;
  const { client, requests } = api(() => {
    if (offline) throw new TypeError('Network request failed');
    return { body: { chatUnread: 2, theme: 'light' } };
  });
  const store = await started(client);
  expect(store.get().status?.chatUnread).toBe(2);
  expect(requests[0].url).toBe('https://pf.example.com/rs/userStatus');
  offline = true;
  await store.refresh();
  expect(store.get().status?.chatUnread).toBe(2);
});

test('a push sets the bell at once and loads the status, again a moment later', async () => {
  let openCount = 1;
  const { client, requests } = api(() => ({ body: { theme: 'light', notifications: { openCount } } }));
  const store = await started(client);
  jest.useFakeTimers();
  try {
    const before = requests.length;
    openCount = 2; // The first request is answered before the commit, the one a moment later has it.
    store.pushed(3);
    expect(store.get().status?.notifications?.openCount).toBe(3);
    await jest.advanceTimersByTimeAsync(0);
    expect(requests.length).toBe(before + 1);
    openCount = 3;
    await jest.advanceTimersByTimeAsync(3_000);
    expect(requests.length).toBe(before + 2);
    expect(store.get().status?.notifications?.openCount).toBe(3);
  } finally {
    jest.useRealTimers();
  }
});

test('the theme of the web pages', () => {
  expect(parseTheme('dark')).toBe('dark');
  expect(parseTheme('light')).toBe('light');
  expect(parseTheme('system')).toBe('system');
  expect(parseTheme(null)).toBe('system');
});

test('the theme of the server is shown and mockStored', async () => {
  const store = await started(api(() => ({ body: { theme: 'dark' } })).client);
  expect(store.get().theme).toBe('dark');
  expect(mockStored.get('theme')).toEqual({ theme: 'dark' });
});

test('an older server is asked by the endpoint of the web pages', async () => {
  const { client } = api((request) =>
    request.url.endsWith('/rs/uiSettings/theme') ? { body: { theme: 'dark' } } : { body: {} },
  );
  expect((await started(client)).get().theme).toBe('dark');
});

test('without the theme of the server the mockStored one is shown', async () => {
  mockStored.set('theme', { theme: 'dark' });
  const { client } = api((request) => (request.url.endsWith('/rs/uiSettings/theme') ? { status: 500 } : { body: {} }));
  expect((await started(client)).get().theme).toBe('dark');
});

test('logged out the system decides', async () => {
  const store = await started(api(() => ({ body: { theme: 'dark' } })).client);
  store.stop();
  expect(store.get().theme).toBe('system');
  expect(store.get().status).toBeNull();
});

test('a theme chosen in the app is shown at once and stored for the user', async () => {
  const { client, requests } = api((request) =>
    request.url.endsWith('/rs/uiSettings/theme') ? { body: { theme: 'dark' } } : { body: { theme: 'light' } },
  );
  const store = await started(client);
  await store.chooseTheme('dark');
  expect(store.get().theme).toBe('dark');
  const post = requests.find((it) => it.url.endsWith('/rs/uiSettings/theme'));
  expect(post?.init.method).toBe('POST');
  expect(post?.body).toEqual({ theme: 'dark' });
  expect(mockStored.get('theme')).toEqual({ theme: 'dark' });
});

test('a theme the server refused to store is taken back', async () => {
  const { client } = api((request) =>
    request.url.endsWith('/rs/uiSettings/theme') ? { status: 500 } : { body: { theme: 'light' } },
  );
  const store = await started(client);
  await expect(store.chooseTheme('dark')).rejects.toBeDefined();
  expect(store.get().theme).toBe('light');
});

test('the push settings are loaded and a choice is stored for the user', async () => {
  const { client, requests } = api((request) =>
    request.url.endsWith('/rs/pushSettings')
      ? request.init.method === 'POST'
        ? { body: { available: true, content: false, writeAccess: true } }
        : { body: { available: true, content: true, writeAccess: true } }
      : { body: { theme: 'light' } },
  );
  const store = await started(client);
  expect(store.get().pushSettings).toBeNull();
  await store.loadPushSettings();
  expect(store.get().pushSettings?.content).toBe(true);
  await store.choosePushContent(false);
  expect(store.get().pushSettings?.content).toBe(false);
  const post = requests.find((it) => it.url.endsWith('/rs/pushSettings') && it.init.method === 'POST');
  expect(post?.body).toEqual({ content: false });
});

test('an older server has no push settings, a refused choice is taken back', async () => {
  let known = false;
  const { client } = api((request) =>
    request.url.endsWith('/rs/pushSettings')
      ? known && request.init.method !== 'POST'
        ? { body: { available: true, content: true } }
        : { status: known ? 500 : 404 }
      : { body: { theme: 'light' } },
  );
  const store = await started(client);
  await store.loadPushSettings();
  expect(store.get().pushSettings).toBeNull();
  known = true;
  await store.loadPushSettings();
  await expect(store.choosePushContent(false)).rejects.toBeDefined();
  expect(store.get().pushSettings?.content).toBe(true);
});
