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

// The essentials of projectforge-flutter/test/core/api_client_test.dart.
import { fakeHttp, FakeAnswer, RecordedRequest } from '../../../testing/fakeHttp';
import { ApiClient } from '../apiClient';
import { PROXY_REFUSED_KEY } from '../apiException';
import { splitSetCookie, withTimeout } from '../http';

function client(handler: (request: RecordedRequest) => FakeAnswer, reauthenticate = async () => true) {
  const { http, requests } = fakeHttp(handler);
  let token = 'token1';
  let proxyCookies: Record<string, string> = { authentik_proxy_x: 'p1' };
  const events = { unauthorized: 0, reauthentications: 0 };
  const api = new ApiClient('https://pf.example.org', {
    http,
    appVersion: '1.0.0',
    token: () => token,
    proxyCookies: () => proxyCookies,
    onUnauthorized: () => events.unauthorized++,
    onReauthenticate: async () => {
      events.reauthentications++;
      const ok = await reauthenticate();
      if (ok) {
        token = 'token2';
        proxyCookies = { authentik_proxy_x: 'p2' };
      }
      return ok;
    },
  });
  return { api, requests, events };
}

test('every request carries the app token and the proxy cookies, no cookie jar, no redirects', async () => {
  const { api, requests } = client(() => ({ body: { ok: true } }));
  expect(await api.get('/rs/userStatus')).toEqual({ ok: true });
  const request = requests[0];
  expect(request.headers.Authorization).toBe('PFApp token1');
  expect(request.headers.Cookie).toBe('authentik_proxy_x=p1');
  expect(request.headers['X-PF-Frontend']).toBe('next');
  expect(request.init.credentials).toBe('omit');
  expect(request.init.redirect).toBe('manual');
});

test('a 401 of the server logs out', async () => {
  const { api, events } = client(() => ({ status: 401, body: { message: 'gone' } }));
  await expect(api.get('/rs/userStatus')).rejects.toMatchObject({ statusCode: 401 });
  expect(events.unauthorized).toBe(1);
  expect(events.reauthentications).toBe(0);
});

test('a second factor too old: new login in the browser and one retry with the new token', async () => {
  const { api, requests, events } = client((request) =>
    request.headers.Authorization === 'PFApp token1'
      ? { status: 403, body: { twoFactorRequired: true } }
      : { body: { ok: 1 } },
  );
  expect(await api.get('/rs/timesheet/appList')).toEqual({ ok: 1 });
  expect(events.reauthentications).toBe(1);
  expect(requests.map((it) => it.headers.Authorization)).toEqual(['PFApp token1', 'PFApp token2']);
});

test('a refusal of the SSO proxy: new login, retry with fresh proxy cookies; never for polling', async () => {
  const refusing = client((request) =>
    request.headers.Cookie === 'authentik_proxy_x=p1'
      ? { status: 302, headers: { location: 'https://sso.example.org/login' } }
      : { body: { ok: 1 } },
  );
  expect(await refusing.api.get('/rs/userStatus')).toEqual({ ok: 1 });
  expect(refusing.events.unauthorized).toBe(0);
  expect(refusing.requests).toHaveLength(2);

  const polling = client(() => ({ status: 401, body: 'login', headers: { 'content-type': 'text/html' } }));
  await expect(polling.api.get('/rs/userStatus', { renewLogin: false })).rejects.toMatchObject({
    messageKey: PROXY_REFUSED_KEY,
  });
  expect(polling.events.reauthentications).toBe(0);
  expect(polling.events.unauthorized).toBe(0);
});

test('a login not satisfying the proxy is not repeated on every request', async () => {
  const { api, events } = client(() => ({ status: 401, body: 'login', headers: { 'content-type': 'text/html' } }));
  await expect(api.get('/rs/a')).rejects.toMatchObject({ messageKey: PROXY_REFUSED_KEY });
  await expect(api.get('/rs/b')).rejects.toMatchObject({ messageKey: PROXY_REFUSED_KEY });
  expect(events.reauthentications).toBe(1);
});

test('the probe of the proxy sends only the proxy cookies, no token', async () => {
  const { api, requests } = client(() => ({ status: 302, headers: { location: 'https://sso.example.org/' } }));
  expect(await api.proxyRefuses()).toBe(true);
  expect(requests[0].url).toBe('https://pf.example.org/next/');
  expect(requests[0].headers.Authorization).toBeUndefined();
  expect(requests[0].headers.Cookie).toBe('authentik_proxy_x=p1');
  expect(requests[0].init.redirect).toBe('manual');
});

test('set-cookie headers joined by commas are split again, not at the comma of a date', () => {
  expect(
    splitSetCookie(
      'JSESSIONID=ABC; Path=/; HttpOnly, remember=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/,x=1',
    ),
  ).toEqual(['JSESSIONID=ABC; Path=/; HttpOnly', 'remember=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/', 'x=1']);
  expect(splitSetCookie(null)).toEqual([]);
});

test('an answer not arriving in time fails as a server not reached', async () => {
  let signal: AbortSignal | undefined;
  const never = withTimeout(
    (_, init) =>
      new Promise<Response>((_, reject) => {
        signal = init.signal ?? undefined;
        signal?.addEventListener('abort', () => reject(new Error('aborted')));
      }),
    10,
  );
  await expect(never('https://pf.example.com/rs/x', {})).rejects.toThrow('No answer within 0.01 s');
  expect(signal?.aborted).toBe(true);
  const fast = withTimeout(async () => new Response('ok'), 1000);
  expect(await (await fast('https://pf.example.com/rs/x', {})).text()).toBe('ok');
});
