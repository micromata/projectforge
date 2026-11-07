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

import { fakeHttp } from '../../../testing/fakeHttp';
import { ApiClient } from '../../api/apiClient';
import { Session } from '../../auth/session';
import { OtherServersStore } from '../otherServers';

const session = (serverUrl: string): Session => ({
  serverUrl,
  token: 't',
  userId: 1,
  deviceId: 2,
  proxyCookies: {},
  appLoginPath: true,
});

function setup(counts: Record<string, number | 'offline'>) {
  const { http, requests } = fakeHttp((request) => {
    const entry = Object.entries(counts).find(([url]) => request.url.startsWith(url));
    if (entry == null || entry[1] === 'offline') throw new TypeError('Network request failed');
    return { body: { notifications: { openCount: entry[1], maxSeverity: 'IMPORTANT' } } };
  });
  const clientFor = (it: Session) =>
    new ApiClient(it.serverUrl, {
      http,
      appVersion: '1',
      token: () => 't',
      proxyCookies: () => ({}),
      onUnauthorized: () => {},
      onReauthenticate: async () => false,
    });
  const store = new OtherServersStore();
  const sessions = Object.keys(counts).map(session);
  return { store, requests, clientFor, sessions };
}

const flush = () => new Promise((resolve) => setTimeout(resolve, 0));

test('the open notifications of the other logins, a push sets its count, one offline keeps its last', async () => {
  const counts: Record<string, number | 'offline'> = { 'https://b.example.com': 2, 'https://c.example.com': 0 };
  const { store, clientFor, sessions } = setup(counts);
  store.start({ sessions: async () => sessions, clientFor });
  await flush();
  expect(store.get().map((it) => it.openCount)).toEqual([2, 0]);
  expect(store.get()[0].maxSeverity).toBe('IMPORTANT');
  expect(store.total()).toBe(2);

  store.pushed(sessions[1], 4);
  expect(store.total()).toBe(6);

  counts['https://b.example.com'] = 'offline';
  await store.refresh();
  expect(store.get().map((it) => it.openCount)).toEqual([2, 0]);

  store.stop();
  expect(store.get()).toEqual([]);
});
