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


// Port of the earlier Flutter app's auth_controller_test.
import { fakeHttp, FakeAnswer, nodeCrypto, RecordedRequest } from '../../../testing/fakeHttp';
import { appLog } from '../../log/appLog';
import { AppLogin } from '../appLogin';
import { AuthController, boundApiClient } from '../authController';
import { Session } from '../session';
import { memoryStore, SessionStore } from '../sessionStore';

jest.mock('../../../../modules/pf-native/src/PfNativeModule', () => ({ __esModule: true, default: {} }));
jest.mock('../secureStorage', () => ({ sessionStore: null }));

const SERVER = 'https://pf.example.com';
const userA: Session = { serverUrl: SERVER, token: 'tokenA', userId: 1, deviceId: 10, proxyCookies: {}, appLoginPath: true };

/**
 * A controller logged in as user A; the browser login answers [userId] and [deviceId] (the user logged in there).
 * [server] answers the requests of the api (not of the login).
 */
async function loggedIn(opts: { userId: number; deviceId: number; server?: (request: RecordedRequest) => FakeAnswer }) {
  const { http, requests } = fakeHttp((request) => {
    if (request.url.endsWith('/rsPublic/systemStatus')) return { body: { appname: 'ProjectForge', version: '9.3' } };
    if (request.url.endsWith('/rsPublic/appAuth/token')) {
      return { body: { userId: opts.userId, deviceId: opts.deviceId, token: 'new' } };
    }
    return opts.server?.(request) ?? { body: {} };
  });
  const store = new SessionStore(memoryStore({}));
  await store.save(userA);
  const counts = { cache: 0, files: 0, cookies: 0 };
  const controller = new AuthController({
    http,
    appLogin: new AppLogin(
      http,
      nodeCrypto,
      async (url) => `projectforge://auth?code=c&state=${new URL(url).searchParams.get('state')}`,
    ),
    sessionStore: store,
    cookieStore: {
      setCookies: async () => [],
      cookieHeader: async () => null,
      clearAll: async () => {
        counts.cookies++;
      },
    },
    cache: { clearAll: () => counts.cache++ },
    files: { clearAll: () => counts.files++ },
    userStatus: { start: async () => undefined, stop: () => undefined },
  });
  await controller.restore();
  expect(controller.getState().session?.userId).toBe(1);
  const apiRequests = () => requests.filter((it) => it.url.includes('/rs/'));
  return { controller, store, counts, apiRequests, requests };
}

test('the start needs no server', async () => {
  const { requests, controller } = await loggedIn({ userId: 1, deviceId: 10 });
  expect(controller.getState().status).toBe('loggedIn');
  expect(requests).toHaveLength(0);
});

test('a renewal of the same app keeps the data', async () => {
  const { controller, counts, apiRequests } = await loggedIn({ userId: 1, deviceId: 10 });
  expect(await controller.reauthenticate()).toBe(true);
  expect(controller.getState().session?.token).toBe('new');
  expect(counts).toEqual({ cache: 0, files: 0, cookies: 0 });
  expect(apiRequests()).toHaveLength(0);
});

test('a renewal as another user logs the old one out and forgets their data', async () => {
  const { controller, store, counts, apiRequests } = await loggedIn({ userId: 2, deviceId: 11 });
  expect(await controller.reauthenticate()).toBe(false);
  const [request] = apiRequests();
  expect(apiRequests()).toHaveLength(1);
  expect(request.url).toBe(`${SERVER}/rs/appAuth/logout`);
  expect(request.headers.Authorization).toBe('PFApp tokenA');
  expect(controller.getState().status).toBe('loggedIn');
  expect(controller.getState().session?.userId).toBe(2);
  expect((await store.load())?.userId).toBe(2);
  expect(counts).toEqual({ cache: 1, files: 1, cookies: 1 });
});

test('the logout reaches the login although the server fails', async () => {
  const { controller, store } = await loggedIn({
    userId: 1,
    deviceId: 10,
    server: () => {
      throw new Error('server down');
    },
  });
  await controller.logout();
  expect(controller.getState().status).toBe('loggedOut');
  expect(await store.load()).toBeNull();
});

test('the logout clears the log of the login', async () => {
  const { controller } = await loggedIn({ userId: 1, deviceId: 10 });
  appLog.info('web', 'Navigation to a page of the user');
  await controller.logout();
  expect(appLog.all().map((it) => it.message)).toEqual(['Logged out: log of the login cleared']);
});

test.each([
  [2, true],
  [1, false],
])('the log of an expired login: user %i logging in next clears it: %s', async (userId, cleared) => {
  const { controller } = await loggedIn({
    userId,
    deviceId: 10,
    server: () => ({ status: 401, body: { status: 401 } }),
  });
  appLog.info('web', 'Navigation to a page of the user');
  await controller.api?.get('/rs/x').catch(() => undefined);
  for (let i = 0; i < 100 && controller.getState().status !== 'loggedOut'; i++) {
    await new Promise((resolve) => setTimeout(resolve, 5));
  }
  expect(controller.getState().status).toBe('loggedOut');
  await controller.login(SERVER);
  expect(controller.getState().session?.userId).toBe(userId);
  const messages = appLog.all().map((it) => it.message);
  if (cleared) expect(messages).not.toContain('Navigation to a page of the user');
  else expect(messages).toContain('Navigation to a page of the user');
});

test('a client of the old server sends nothing of the new login and cannot end it', async () => {
  const serverB: Session = {
    serverUrl: 'https://other.example.com',
    token: 'tokenB',
    userId: 2,
    deviceId: 20,
    proxyCookies: { proxy: 'secretB' },
    appLoginPath: true,
  };
  const { controller, counts } = await loggedIn({ userId: 1, deviceId: 10 });
  const { http, requests: seen } = fakeHttp(() => ({ body: {} }));
  let unauthorized = 0;
  let renewals = 0;
  const old = boundApiClient(userA, {
    http,
    current: () => controller.getState().session,
    onUnauthorized: () => unauthorized++,
    onReauthenticate: async () => {
      renewals++;
      return true;
    },
  });
  await old.post('/rs/x', { renewLogin: false });
  expect(seen[seen.length - 1].headers.Authorization).toBe('PFApp tokenA');

  await controller.switchTo(serverB);
  expect(controller.getState().session?.token).toBe('tokenB');
  // The cache goes, the WebView data stays (per server, like the stored login).
  expect(counts.cache).toBe(1);
  expect(counts.cookies).toBe(0);
  await old.post('/rs/x', { renewLogin: false });
  expect(seen[seen.length - 1].headers.Authorization).toBeUndefined();
  expect(seen[seen.length - 1].headers.Cookie).toBeUndefined();
  // A late refusal or renewal of the old server doesn't touch the new login.
  const refusing = boundApiClient(userA, {
    http: fakeHttp(() => ({ status: 401, body: { status: 401 } })).http,
    current: () => controller.getState().session,
    onUnauthorized: () => unauthorized++,
    onReauthenticate: async () => {
      renewals++;
      return true;
    },
  });
  await refusing.get('/rs/x').catch(() => undefined);
  expect(unauthorized).toBe(0);
  expect(renewals).toBe(0);
  expect(controller.getState().status).toBe('loggedIn');
});
